package com.hhoa.kline.core.core.task.handler;

import static com.hhoa.kline.core.core.integrations.misc.ExtractText.processFilesIntoText;

import com.hhoa.kline.core.core.integrations.subagents.SubagentCommandUtils;
import com.hhoa.kline.core.core.integrations.terminal.TerminalInfo;
import com.hhoa.kline.core.core.integrations.terminal.TerminalManager;
import com.hhoa.kline.core.core.integrations.terminal.TerminalProcessListeners;
import com.hhoa.kline.core.core.integrations.terminal.TerminalProcessResultPromise;
import com.hhoa.kline.core.core.services.telemetry.TelemetryService;
import com.hhoa.kline.core.core.shared.ClineAsk;
import com.hhoa.kline.core.core.shared.ClineSay;
import com.hhoa.kline.core.core.shared.TerminalExecutionMode;
import com.hhoa.kline.core.core.storage.StateManager;
import com.hhoa.kline.core.core.task.ClineMessage;
import com.hhoa.kline.core.core.task.MessageStateHandler;
import com.hhoa.kline.core.core.task.TaskState;
import com.hhoa.kline.core.core.task.deps.ActiveBackgroundCommand;
import com.hhoa.kline.core.core.task.tools.types.ToolContext;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class TaskV2CommandHandler {

    private final TerminalManager terminalManager;
    private final TelemetryService telemetryService;
    private final StateManager stateManager;
    private final String cwd;
    private final TaskState taskState;
    private final String taskId;
    private final String ulid;
    private final MessageStateHandler messageStateHandler;
    private final Supplier<ActiveBackgroundCommand> getActiveBackgroundCommand;
    private final Consumer<ActiveBackgroundCommand> setActiveBackgroundCommand;
    private final java.util.function.BiConsumer<Boolean, String> updateBackgroundCommandState;
    private final TaskV2SayAskHandler sayAskHandler;
    private final Supplier<Boolean> shouldShowBackgroundTerminalSuggestion;

    public TaskV2CommandHandler(
            TerminalManager terminalManager,
            TelemetryService telemetryService,
            StateManager stateManager,
            String cwd,
            TaskState taskState,
            String taskId,
            String ulid,
            MessageStateHandler messageStateHandler,
            Supplier<ActiveBackgroundCommand> getActiveBackgroundCommand,
            Consumer<ActiveBackgroundCommand> setActiveBackgroundCommand,
            java.util.function.BiConsumer<Boolean, String> updateBackgroundCommandState,
            TaskV2SayAskHandler sayAskHandler,
            Supplier<Boolean> shouldShowBackgroundTerminalSuggestion) {
        this.terminalManager = terminalManager;
        this.telemetryService = telemetryService;
        this.stateManager = stateManager;
        this.cwd = cwd;
        this.taskState = taskState;
        this.taskId = taskId;
        this.ulid = ulid;
        this.messageStateHandler = messageStateHandler;
        this.getActiveBackgroundCommand = getActiveBackgroundCommand;
        this.setActiveBackgroundCommand = setActiveBackgroundCommand;
        this.updateBackgroundCommandState = updateBackgroundCommandState;
        this.sayAskHandler = sayAskHandler;
        this.shouldShowBackgroundTerminalSuggestion = shouldShowBackgroundTerminalSuggestion;
    }

    public ToolContext.ExecuteResult executeCommandTool(String command, Integer timeoutSeconds) {
        ToolContext.ExecuteResult er = new ToolContext.ExecuteResult();
        er.userRejected = false;
        if (command == null || command.trim().isEmpty()) {
            er.result = "(empty command)";
            return er;
        }

        try {
            boolean isSubagent = SubagentCommandUtils.isSubagentCommand(command);
            long subAgentStartTime = isSubagent ? System.currentTimeMillis() : 0;

            if (SubagentCommandUtils.transformClineCommand(command) != command && isSubagent) {
                command = SubagentCommandUtils.transformClineCommand(command);
            }

            log.info("Executing command in terminal: {}", command);
            final String finalCommand = command;

            TerminalManager terminalManagerToUse = terminalManager;
            if (isSubagent) {
                terminalManagerToUse = new TerminalManager();
                terminalManagerToUse.setShellIntegrationTimeout(
                        terminalManager.getShellIntegrationTimeout());
                terminalManagerToUse.setTerminalReuseEnabled(
                        terminalManager.isTerminalReuseEnabled());
                terminalManagerToUse.setTerminalOutputLineLimit(
                        terminalManager.getTerminalOutputLineLimit());
                terminalManagerToUse.setSubagentTerminalOutputLineLimit(
                        terminalManager.getSubagentTerminalOutputLineLimit());
            }

            TerminalInfo terminalInfo = terminalManagerToUse.getOrCreateTerminal(cwd).join();
            terminalInfo.getTerminal().show();

            final int CHUNK_LINE_COUNT = 20;
            final int CHUNK_BYTE_SIZE = 2048;
            final long CHUNK_DEBOUNCE_MS = 100;
            final long BUFFER_STUCK_TIMEOUT_MS = 6000;
            final long COMPLETION_TIMEOUT_MS = 6000;

            List<String> outputLines = new ArrayList<>();
            List<String> outputBuffer = new ArrayList<>();
            AtomicInteger bufferBytes = new AtomicInteger(0);
            AtomicBoolean didContinue = new AtomicBoolean(false);
            AtomicBoolean didCancelViaUi = new AtomicBoolean(false);
            AtomicBoolean completed = new AtomicBoolean(false);
            AtomicReference<ScheduledFuture<?>> chunkTimer = new AtomicReference<>();
            AtomicReference<ScheduledFuture<?>> bufferStuckTimer = new AtomicReference<>();
            AtomicReference<ScheduledFuture<?>> completionTimer = new AtomicReference<>();
            ScheduledExecutorService debounceExecutor =
                    Executors.newSingleThreadScheduledExecutor();

            UserFeedback userFeedback = new UserFeedback();

            final List<String> finalOutputBuffer = outputBuffer;
            final AtomicInteger finalBufferBytes = bufferBytes;
            final AtomicBoolean finalDidContinue = didContinue;
            final AtomicBoolean finalDidCancelViaUi = didCancelViaUi;
            final AtomicReference<ScheduledFuture<?>> finalChunkTimer = chunkTimer;
            final AtomicReference<ScheduledFuture<?>> finalBufferStuckTimer = bufferStuckTimer;

            final AtomicReference<Consumer<Boolean>> flushBufferRef = new AtomicReference<>();
            final AtomicReference<TerminalProcessResultPromise> processRef =
                    new AtomicReference<>();

            TerminalProcessListeners listeners = new TerminalProcessListeners();

            flushBufferRef.set(
                    (force) -> {
                        if (finalOutputBuffer.isEmpty() && !force) {
                            return;
                        }
                        String chunk = String.join("\n", finalOutputBuffer);
                        finalOutputBuffer.clear();
                        finalBufferBytes.set(0);

                        ScheduledFuture<?> timer = finalChunkTimer.getAndSet(null);
                        if (timer != null) {
                            timer.cancel(false);
                        }

                        ScheduledFuture<?> stuckTimer = finalBufferStuckTimer.getAndSet(null);
                        if (stuckTimer != null) {
                            stuckTimer.cancel(false);
                        }

                        ScheduledFuture<?> newStuckTimer =
                                debounceExecutor.schedule(
                                        () -> {
                                            telemetryService.captureTerminalHang("buffer_stuck");
                                            finalBufferStuckTimer.set(null);
                                        },
                                        BUFFER_STUCK_TIMEOUT_MS,
                                        TimeUnit.MILLISECONDS);
                        finalBufferStuckTimer.set(newStuckTimer);

                        try {
                            //                            AskResult askResult =
                            //                                    sayAskHandler.ask(
                            //                                            ClineAsk.COMMAND_OUTPUT,
                            //                                            chunk);
                            //                            String responseText = askResult.getText();
                            //                            List<String> responseImages =
                            // askResult.getImages();
                            //                            List<String> responseFiles =
                            // askResult.getFiles();
                            //
                            //                            if
                            // (ClineAskResponse.YES_BUTTON_CLICKED.equals(
                            //                                    askResult.getResponse())) {
                            //
                            // telemetryService.captureTerminalUserIntervention(
                            //                                        "process_while_running");
                            //                                finalDidContinue.set(true);
                            //                                TerminalProcessResultPromise process =
                            // processRef.get();
                            //                                if (process != null) {
                            //                                    process.continueExecution();
                            //                                }
                            //
                            //                                if (responseText != null
                            //                                        || (responseImages != null &&
                            // !responseImages.isEmpty())
                            //                                        || (responseFiles != null &&
                            // !responseFiles.isEmpty())) {
                            //                                    userFeedback.text = responseText;
                            //                                    userFeedback.images =
                            // responseImages;
                            //                                    userFeedback.files =
                            // responseFiles;
                            //                                }
                            //
                            //                                if (finalDidCancelViaUi.get()) {
                            //                                    finalOutputBuffer.clear();
                            //                                    finalBufferBytes.set(0);
                            //                                    sayAskHandler.say(
                            //                                            ClineSay.COMMAND_OUTPUT,
                            //                                            "Command cancelled",
                            //                                            null,
                            //                                            null,
                            //                                            null);
                            //                                }
                            //
                            //                                if (!finalDidCancelViaUi.get() &&
                            // !finalOutputBuffer.isEmpty()) {
                            //
                            // flushBufferRef.get().accept(false);
                            //                                }
                            //                            } else if
                            // (ClineAskResponse.NO_BUTTON_CLICKED.equals(
                            //                                            askResult.getResponse())
                            //                                    &&
                            // ExtensionMessageConstants.COMMAND_CANCEL_TOKEN.equals(
                            //                                            responseText)) {
                            //
                            // telemetryService.captureTerminalUserIntervention("cancelled");
                            //                                finalDidCancelViaUi.set(true);
                            //                                finalOutputBuffer.clear();
                            //                                finalBufferBytes.set(0);
                            //                                sayAskHandler.say(
                            //                                        ClineSay.COMMAND_OUTPUT,
                            //                                        "Command cancelled",
                            //                                        null,
                            //                                        null,
                            //                                        null);
                            //                            } else {
                            //                                userFeedback.text = responseText;
                            //                                userFeedback.images = responseImages;
                            //                                userFeedback.files = responseFiles;
                            //                            }
                        } catch (Exception e) {
                            log.error(
                                    "Error while asking for command output: {}", e.getMessage(), e);
                        } finally {
                            ScheduledFuture<?> stuckTimerToClear =
                                    finalBufferStuckTimer.getAndSet(null);
                            if (stuckTimerToClear != null) {
                                stuckTimerToClear.cancel(false);
                            }
                        }
                    });

            listeners.onLine(
                    line -> {
                        if (didCancelViaUi.get()) {
                            return;
                        }
                        outputLines.add(line);

                        if (!didContinue.get()) {
                            outputBuffer.add(line);
                            bufferBytes.addAndGet(line.getBytes(StandardCharsets.UTF_8).length + 1);

                            if (outputBuffer.size() >= CHUNK_LINE_COUNT
                                    || bufferBytes.get() >= CHUNK_BYTE_SIZE) {
                                flushBufferRef.get().accept(false);
                            } else {
                                ScheduledFuture<?> timer = finalChunkTimer.get();
                                if (timer != null) {
                                    timer.cancel(false);
                                }
                                ScheduledFuture<?> newTimer =
                                        debounceExecutor.schedule(
                                                () -> flushBufferRef.get().accept(false),
                                                CHUNK_DEBOUNCE_MS,
                                                TimeUnit.MILLISECONDS);
                                finalChunkTimer.set(newTimer);
                            }
                        } else {
                            sayAskHandler.say(ClineSay.COMMAND_OUTPUT, line, null, null, null);
                        }
                    });

            listeners.onCompleted(
                    () -> {
                        completed.set(true);

                        ScheduledFuture<?> timer = finalChunkTimer.getAndSet(null);
                        if (timer != null) {
                            timer.cancel(false);
                        }

                        ScheduledFuture<?> completionTimerToClear = completionTimer.getAndSet(null);
                        if (completionTimerToClear != null) {
                            completionTimerToClear.cancel(false);
                        }

                        if (!finalDidContinue.get() && !finalOutputBuffer.isEmpty()) {
                            flushBufferRef.get().accept(true);
                        }
                    });

            TerminalExecutionMode terminalExecutionMode =
                    stateManager.getGlobalState().getTerminalExecutionMode();
            if (terminalExecutionMode == null) {
                terminalExecutionMode = TerminalExecutionMode.VSCODE_TERMINAL;
            }
            final TerminalExecutionMode finalTerminalExecutionMode = terminalExecutionMode;
            Runnable clearCommandState =
                    () -> {
                        if (finalTerminalExecutionMode == TerminalExecutionMode.BACKGROUND_EXEC) {
                            if (getActiveBackgroundCommand.get() == null) {
                                return;
                            }
                            setActiveBackgroundCommand.accept(null);
                        }
                        if (updateBackgroundCommandState != null) {
                            updateBackgroundCommandState.accept(false, taskId);
                        }
                        List<ClineMessage> clineMessages = messageStateHandler.getClineMessages();
                        int lastCommandIndex = -1;
                        for (int i = clineMessages.size() - 1; i >= 0; i--) {
                            ClineMessage msg = clineMessages.get(i);
                            if ((ClineAsk.COMMAND.equals(msg.getAsk())
                                    || ClineSay.COMMAND.equals(msg.getSay()))) {
                                lastCommandIndex = i;
                                break;
                            }
                        }
                        if (lastCommandIndex != -1) {
                            ClineMessage commandMessage = clineMessages.get(lastCommandIndex);
                            commandMessage.setCommandCompleted(true);
                            messageStateHandler.updateClineMessage(
                                    lastCommandIndex, commandMessage);
                        }
                    };
            listeners.onError(clearCommandState);
            listeners.onCompleted(clearCommandState);

            listeners.onNoShellIntegration(
                    () -> {
                        boolean shouldShowSuggestion = false;
                        if (shouldShowBackgroundTerminalSuggestion != null) {
                            shouldShowSuggestion =
                                    Boolean.TRUE.equals(
                                            shouldShowBackgroundTerminalSuggestion.get());
                        }
                        if (shouldShowSuggestion) {
                            sayAskHandler.say(
                                    ClineSay.SHELL_INTEGRATION_WARNING_WITH_SUGGESTION,
                                    null,
                                    null,
                                    null,
                                    null);
                        } else {
                            sayAskHandler.say(
                                    ClineSay.SHELL_INTEGRATION_WARNING, null, null, null, null);
                        }
                    });

            TerminalProcessResultPromise process =
                    terminalManagerToUse.runCommand(terminalInfo, command, listeners);
            processRef.set(process);

            if (updateBackgroundCommandState != null) {
                updateBackgroundCommandState.accept(true, taskId);
            }

            if (terminalExecutionMode == TerminalExecutionMode.BACKGROUND_EXEC) {
                ActiveBackgroundCommand activeCmd = new ActiveBackgroundCommand();
                activeCmd.setProcess(process);
                activeCmd.setCommand(command);
                setActiveBackgroundCommand.accept(activeCmd);
            }

            completionTimer.set(
                    debounceExecutor.schedule(
                            () -> {
                                if (!completed.get()) {
                                    telemetryService.captureTerminalHang("waiting_for_completion");
                                    completionTimer.set(null);
                                }
                            },
                            COMPLETION_TIMEOUT_MS,
                            TimeUnit.MILLISECONDS));

            if (timeoutSeconds != null && timeoutSeconds > 0) {
                try {
                    process.getPromise().get(timeoutSeconds, TimeUnit.SECONDS);
                } catch (TimeoutException e) {
                    log.warn("Command timed out after {} seconds", timeoutSeconds);
                }
            }

            try {
                process.getPromise().get(COMPLETION_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                // ignore
            }

            Thread.sleep(100);

            String result =
                    terminalManagerToUse.processOutput(
                            outputLines,
                            isSubagent
                                    ? terminalManagerToUse.getSubagentTerminalOutputLineLimit()
                                    : null,
                            isSubagent);

            if (isSubagent && subAgentStartTime > 0) {
                long durationMs = System.currentTimeMillis() - subAgentStartTime;
                telemetryService.captureSubagentExecution(
                        ulid, durationMs, outputLines.size(), completed.get());
            }

            if (didCancelViaUi.get()) {
                er.result =
                        "Command cancelled. "
                                + (result.length() > 0
                                        ? "\nOutput captured before cancellation:\n" + result
                                        : "");
                er.userRejected = true;
            } else if (userFeedback.hasFeedback()) {
                sayAskHandler.say(
                        ClineSay.USER_FEEDBACK,
                        userFeedback.text,
                        userFeedback.images,
                        userFeedback.files,
                        null);

                String fileContentString = "";
                if (userFeedback.files != null && !userFeedback.files.isEmpty()) {
                    fileContentString =
                            processFilesIntoText(
                                    userFeedback.files.stream()
                                            .map(Path::of)
                                            .collect(Collectors.toList()));
                }

                StringBuilder feedbackResult = new StringBuilder();
                feedbackResult.append("Command is still running in the user's terminal.");
                if (result.length() > 0) {
                    feedbackResult.append("\nHere's the output so far:\n").append(result);
                }
                feedbackResult.append(
                        "\n\nThe user provided the following feedback:\n<feedback>\n");
                if (userFeedback.text != null) {
                    feedbackResult.append(userFeedback.text);
                }
                feedbackResult.append("\n</feedback>");

                er.result = feedbackResult.toString();
                if (fileContentString != null && !fileContentString.isEmpty()) {
                    er.result += "\n\n" + fileContentString;
                }
            } else if (completed.get()) {
                er.result =
                        "Command executed." + (result.length() > 0 ? "\nOutput:\n" + result : "");
            } else {
                er.result =
                        "Command is still running in the user's terminal."
                                + (result.length() > 0
                                        ? "\nHere's the output so far:\n" + result
                                        : "")
                                + "\n\nYou will be updated on the terminal status and new output in the future.";
            }

            ScheduledFuture<?> timer = chunkTimer.getAndSet(null);
            if (timer != null) {
                timer.cancel(false);
            }
            ScheduledFuture<?> stuckTimer = bufferStuckTimer.getAndSet(null);
            if (stuckTimer != null) {
                stuckTimer.cancel(false);
            }

            if (isSubagent && terminalManagerToUse != terminalManager) {
                try {
                    terminalManagerToUse.close();
                } catch (Exception e) {
                    log.error("Failed to close subagent terminal manager: {}", e.getMessage(), e);
                }
            }

            debounceExecutor.shutdown();

            return er;
        } catch (Exception e) {
            er.result = "Error executing command: " + e.getMessage();
            log.error("Error executing command: {}", e.getMessage(), e);
            return er;
        }
    }

    public Boolean cancelBackgroundCommand() {
        TerminalExecutionMode terminalExecutionMode =
                stateManager.getGlobalState().getTerminalExecutionMode();
        if (terminalExecutionMode == null) {
            terminalExecutionMode = TerminalExecutionMode.VSCODE_TERMINAL;
        }
        if (terminalExecutionMode != TerminalExecutionMode.BACKGROUND_EXEC) {
            return false;
        }
        if (getActiveBackgroundCommand.get() == null) {
            return false;
        }
        ActiveBackgroundCommand activeCmd = getActiveBackgroundCommand.get();
        setActiveBackgroundCommand.accept(null);

        if (updateBackgroundCommandState != null) {
            updateBackgroundCommandState.accept(false, taskId);
        }

        try {
            activeCmd.getProcess().getProcess().shutdown();
        } catch (Exception e) {
            log.error("Failed to terminate background command: " + e.getMessage());
            return false;
        }

        try {
            sayAskHandler.say(
                    ClineSay.COMMAND_OUTPUT,
                    "Command cancelled. Background execution has been terminated.",
                    null,
                    null,
                    null);
            return true;
        } catch (Exception e) {
            log.error("Failed to cancel background command: " + e.getMessage());
            return false;
        }
    }

    private static class UserFeedback {
        String text;
        List<String> images;
        List<String> files;

        boolean hasFeedback() {
            return (text != null && !text.isEmpty())
                    || (images != null && !images.isEmpty())
                    || (files != null && !files.isEmpty());
        }
    }
}
