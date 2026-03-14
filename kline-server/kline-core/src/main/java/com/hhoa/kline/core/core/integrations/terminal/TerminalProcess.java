package com.hhoa.kline.core.core.integrations.terminal;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

/** 终端进程类，处理命令执行和输出流 */
@Slf4j
public class TerminalProcess {
    private static final long PROCESS_HOT_TIMEOUT_NORMAL = 2000;
    private static final long PROCESS_HOT_TIMEOUT_COMPILING = 15000;

    private boolean isListening = true;
    private StringBuilder buffer = new StringBuilder();
    private StringBuilder fullOutput = new StringBuilder();
    private int lastRetrievedIndex = 0;
    private boolean isHot = false;
    private ScheduledFuture<?> hotTimer;

    private final List<LineListener> lineListeners = new ArrayList<>();
    private final List<Runnable> continueListeners = new ArrayList<>();
    private final List<Runnable> completedListeners = new ArrayList<>();
    private final List<ErrorListener> errorListeners = new ArrayList<>();
    private final List<Runnable> noShellIntegrationListeners = new ArrayList<>();

    // Flags to track if events have already been emitted (for late listener registration)
    private volatile boolean completedEmitted = false;
    private volatile boolean continueEmitted = false;
    private volatile boolean noShellIntegrationEmitted = false;
    private volatile Throwable errorEmitted = null;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    /** 行输出监听器 */
    @FunctionalInterface
    public interface LineListener {
        void onLine(String line);
    }

    /** 错误监听器 */
    @FunctionalInterface
    public interface ErrorListener {
        void onError(Throwable error);
    }

    /** 添加行监听器 */
    public void onLine(LineListener listener) {
        synchronized (lineListeners) {
            lineListeners.add(listener);
        }
    }

    /** 添加继续监听器（一次性） */
    public void onceContinue(Runnable listener) {
        registerOnceListener(continueListeners, () -> continueEmitted, listener, Runnable::run);
    }

    /** 添加完成监听器（一次性） */
    public void onceCompleted(Runnable listener) {
        registerOnceListener(completedListeners, () -> completedEmitted, listener, Runnable::run);
    }

    /** 添加错误监听器（一次性） */
    public void onceError(ErrorListener listener) {
        synchronized (errorListeners) {
            if (errorEmitted != null) {
                listener.onError(errorEmitted);
            } else {
                errorListeners.add(listener);
            }
        }
    }

    /** 添加无 Shell 集成监听器（一次性） */
    public void onceNoShellIntegration(Runnable listener) {
        registerOnceListener(
                noShellIntegrationListeners,
                () -> noShellIntegrationEmitted,
                listener,
                Runnable::run);
    }

    /** 通用的一次性监听器注册方法 */
    private <T> void registerOnceListener(
            List<T> listeners,
            java.util.function.Supplier<Boolean> emittedGetter,
            T listener,
            java.util.function.Consumer<T> executor) {
        synchronized (listeners) {
            if (emittedGetter.get()) {
                executor.accept(listener);
            } else {
                listeners.add(listener);
            }
        }
    }

    /** 移除所有行监听器 */
    public void removeAllLineListeners() {
        synchronized (lineListeners) {
            lineListeners.clear();
        }
    }

    /** 运行命令 */
    public CompletableFuture<Void> run(Terminal terminal, String command) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        Terminal.ShellIntegration shellIntegration = terminal.getShellIntegration();
        if (shellIntegration != null && shellIntegration.getCwd() != null) {
            executeCommand(terminal, command, shellIntegration, future);
        } else {
            terminal.sendText(command, true);
            scheduler.schedule(
                    () -> {
                        try {
                            String fallbackOutput = GetLatestOutput.getLatestTerminalOutput();
                            if (fallbackOutput != null && !fallbackOutput.trim().isEmpty()) {
                                String message =
                                        "The command's output could not be captured due to some technical issue, "
                                                + "however it has been executed successfully. Here's the current terminal's content "
                                                + "to help you get the command's output:\n\n"
                                                + fallbackOutput;
                                emitLine(message);
                            }
                        } catch (Exception e) {
                            log.error("Error capturing terminal output", e);
                        }
                        emitCompleted();
                        emitContinue();
                        emitNoShellIntegration();
                        future.complete(null);
                    },
                    3,
                    TimeUnit.SECONDS);
        }

        return future;
    }

    private void executeCommand(
            Terminal terminal,
            String command,
            Terminal.ShellIntegration shellIntegration,
            CompletableFuture<Void> future) {
        Terminal.CommandExecution execution = shellIntegration.executeCommand(command);

        boolean[] isFirstChunk = {true};
        boolean[] didOutputNonCommand = {false};
        boolean[] didEmitEmptyLine = {false};
        boolean[] shouldBreak = {false};

        CompletableFuture<Void> readFuture =
                execution.read(
                        data -> {
                            try {
                                if (shouldBreak[0]) {
                                    return;
                                }

                                if (isFirstChunk[0]) {
                                    data = processFirstChunk(data, command);
                                    isFirstChunk[0] = false;
                                } else {
                                    data = AnsiUtils.stripAnsi(data);
                                }

                                if (data.contains("^C") || data.contains("\u0003")) {
                                    if (hotTimer != null) {
                                        hotTimer.cancel(false);
                                    }
                                    isHot = false;
                                    shouldBreak[0] = true;
                                    return;
                                }

                                if (!didOutputNonCommand[0]) {
                                    data = removeCommandEcho(data, command);
                                    if (!data.isEmpty()) {
                                        didOutputNonCommand[0] = true;
                                    }
                                }

                                updateHotState(data);

                                if (!didEmitEmptyLine[0]
                                        && fullOutput.length() == 0
                                        && !data.isEmpty()) {
                                    emitLine("");
                                    didEmitEmptyLine[0] = true;
                                }

                                fullOutput.append(data);
                                if (isListening) {
                                    emitIfEol(data);
                                    lastRetrievedIndex = fullOutput.length() - buffer.length();
                                }
                            } catch (Exception e) {
                                log.error("Error processing command output", e);
                                emitError(e);
                            }
                        });

        // Add timeout to prevent hanging (30 seconds default timeout)
        ScheduledFuture<?> timeoutTask =
                scheduler.schedule(
                        () -> {
                            if (!readFuture.isDone() && !future.isDone()) {
                                log.warn(
                                        "Command execution timeout (readFuture not completed after 30s), forcing completion. Command: {}",
                                        command);
                                emitRemainingBufferIfListening();

                                if (hotTimer != null) {
                                    hotTimer.cancel(false);
                                }
                                isHot = false;

                                emitCompleted();
                                emitContinue();
                                future.complete(null);
                            }
                        },
                        30,
                        TimeUnit.SECONDS);

        readFuture
                .thenRun(
                        () -> {
                            // Cancel timeout if readFuture completes normally
                            timeoutTask.cancel(false);

                            emitRemainingBufferIfListening();

                            if (fullOutput.toString().trim().isEmpty()) {
                                try {
                                    String terminalSnapshot =
                                            GetLatestOutput.getLatestTerminalOutput();
                                    if (terminalSnapshot != null
                                            && !terminalSnapshot.trim().isEmpty()) {
                                        String fallbackMessage =
                                                "The command's output could not be captured due to some technical issue, "
                                                        + "however it has been executed successfully. Here's the current terminal's content "
                                                        + "to help you get the command's output:\n\n"
                                                        + terminalSnapshot;
                                        emitLine(fallbackMessage);
                                    }
                                } catch (Exception e) {
                                    log.error("Error capturing terminal output", e);
                                }
                            }

                            if (hotTimer != null) {
                                hotTimer.cancel(false);
                            }
                            isHot = false;

                            emitCompleted();
                            emitContinue();
                            future.complete(null);
                        })
                .exceptionally(
                        error -> {
                            // Cancel timeout if readFuture fails
                            timeoutTask.cancel(false);

                            emitError(error);
                            future.completeExceptionally(error);
                            return null;
                        });
    }

    private String processFirstChunk(String data, String command) {
        Pattern vscodeSequencePattern = Pattern.compile("\\x1b\\]633;[^\\x07]*\\x07");
        Matcher matcher = vscodeSequencePattern.matcher(data);

        String outputBetweenSequences = "";
        Pattern sequencePattern = Pattern.compile("\\]633;C([\\s\\S]*?)\\]633;D");
        Matcher seqMatcher = sequencePattern.matcher(data);
        if (seqMatcher.find()) {
            outputBetweenSequences = removeLastLineArtifacts(seqMatcher.group(1)).trim();
        }

        int lastIndex = -1;
        while (matcher.find()) {
            lastIndex = matcher.end();
        }
        if (lastIndex >= 0) {
            data = data.substring(lastIndex);
        }

        if (!outputBetweenSequences.isEmpty()) {
            data = outputBetweenSequences + "\n" + data;
        }

        data = AnsiUtils.stripAnsi(data);
        String[] lines = data.split("\n");
        if (lines.length > 0) {
            lines[0] = lines[0].replaceAll("[^\\x20-\\x7E]", "");
            if (lines[0].length() >= 2
                    && lines[0].charAt(0) == lines[0].charAt(1)
                    && !"[{\"\'<(".contains(String.valueOf(lines[0].charAt(0)))) {
                lines[0] = lines[0].substring(1);
            }
            lines[0] = lines[0].replaceAll("^[\\x00-\\x1F%$#>\\s]*", "");
            if (lines.length > 1) {
                lines[1] = lines[1].replaceAll("^[\\x00-\\x1F%$#>\\s]*", "");
            }
            data = String.join("\n", lines);
        }

        return data;
    }

    private String removeCommandEcho(String data, String command) {
        String[] lines = data.split("\n");
        List<String> filtered = new ArrayList<>();
        for (String line : lines) {
            if (!command.contains(line.trim())) {
                filtered.add(line);
            }
        }
        return String.join("\n", filtered);
    }

    private void updateHotState(String data) {
        isHot = true;
        if (hotTimer != null) {
            hotTimer.cancel(false);
        }

        String lowerData = data.toLowerCase();
        String[] compilingMarkers = {
            "compiling", "building", "bundling", "transpiling", "generating", "starting"
        };
        String[] markerNullifiers = {
            "compiled",
            "success",
            "finish",
            "complete",
            "succeed",
            "done",
            "end",
            "stop",
            "exit",
            "terminate",
            "error",
            "fail"
        };

        boolean isCompiling = false;
        for (String marker : compilingMarkers) {
            if (lowerData.contains(marker)) {
                isCompiling = true;
                break;
            }
        }
        if (isCompiling) {
            for (String nullifier : markerNullifiers) {
                if (lowerData.contains(nullifier)) {
                    isCompiling = false;
                    break;
                }
            }
        }

        long timeout = isCompiling ? PROCESS_HOT_TIMEOUT_COMPILING : PROCESS_HOT_TIMEOUT_NORMAL;
        hotTimer = scheduler.schedule(() -> isHot = false, timeout, TimeUnit.MILLISECONDS);
    }

    private void emitIfEol(String chunk) {
        buffer.append(chunk);
        int lineEndIndex;
        while ((lineEndIndex = buffer.indexOf("\n")) != -1) {
            String line = buffer.substring(0, lineEndIndex).replaceAll("\\r$", "");
            emitLine(line);
            buffer.delete(0, lineEndIndex + 1);
        }
    }

    private void emitRemainingBufferIfListening() {
        if (buffer.length() > 0 && isListening) {
            String remaining = removeLastLineArtifacts(buffer.toString());
            if (!remaining.isEmpty()) {
                emitLine(remaining);
            }
            buffer.setLength(0);
            lastRetrievedIndex = fullOutput.length();
        }
    }

    private void emitLine(String line) {
        List<LineListener> listenersToExecute;
        synchronized (lineListeners) {
            listenersToExecute = new ArrayList<>(lineListeners);
        }
        executeListeners(listenersToExecute, listener -> listener.onLine(line), "line listener");
    }

    private void emitContinue() {
        emitOnceEvent(
                continueListeners,
                () -> continueEmitted,
                () -> continueEmitted = true,
                Runnable::run,
                "continue listener");
    }

    private void emitCompleted() {
        emitOnceEvent(
                completedListeners,
                () -> completedEmitted,
                () -> completedEmitted = true,
                Runnable::run,
                "completed listener");
    }

    private void emitError(Throwable error) {
        List<ErrorListener> listenersToExecute;
        synchronized (errorListeners) {
            if (errorEmitted != null) {
                return; // Already emitted, avoid duplicate execution
            }
            errorEmitted = error;
            listenersToExecute = new ArrayList<>(errorListeners);
            errorListeners.clear();
        }
        executeListeners(listenersToExecute, listener -> listener.onError(error), "error listener");
    }

    private void emitNoShellIntegration() {
        emitOnceEvent(
                noShellIntegrationListeners,
                () -> noShellIntegrationEmitted,
                () -> noShellIntegrationEmitted = true,
                Runnable::run,
                "no shell integration listener");
    }

    /** 通用的一次性事件触发方法 */
    private <T> void emitOnceEvent(
            List<T> listeners,
            java.util.function.Supplier<Boolean> emittedGetter,
            Runnable emittedSetter,
            java.util.function.Consumer<T> executor,
            String listenerName) {
        List<T> listenersToExecute;
        synchronized (listeners) {
            if (emittedGetter.get()) {
                return; // Already emitted, avoid duplicate execution
            }
            emittedSetter.run();
            listenersToExecute = new ArrayList<>(listeners);
            listeners.clear();
        }
        executeListeners(listenersToExecute, executor, listenerName);
    }

    /** 通用监听器执行方法 */
    private <T> void executeListeners(
            List<T> listeners, java.util.function.Consumer<T> executor, String listenerName) {
        for (T listener : listeners) {
            try {
                executor.accept(listener);
            } catch (Exception e) {
                log.error("Error in {}", listenerName, e);
            }
        }
    }

    /** 继续执行（停止监听新输出） */
    public void continueExecution() {
        emitRemainingBufferIfListening();
        isListening = false;
        removeAllLineListeners();
        emitContinue();
    }

    /** 获取未检索的输出 */
    public String getUnretrievedOutput() {
        String unretrieved = fullOutput.substring(lastRetrievedIndex);
        lastRetrievedIndex = fullOutput.length();
        return removeLastLineArtifacts(unretrieved);
    }

    /** 移除最后一行的人为痕迹 */
    private String removeLastLineArtifacts(String output) {
        String[] lines = output.trim().split("\n");
        if (lines.length > 0) {
            lines[lines.length - 1] = lines[lines.length - 1].replaceAll("[%$#>]\\s*$", "");
        }
        return String.join("\n", lines).trim();
    }

    public boolean isHot() {
        return isHot;
    }

    public void shutdown() {
        if (hotTimer != null) {
            hotTimer.cancel(false);
        }
        scheduler.shutdown();
    }
}
