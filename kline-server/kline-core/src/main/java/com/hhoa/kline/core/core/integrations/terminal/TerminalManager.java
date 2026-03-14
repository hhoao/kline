package com.hhoa.kline.core.core.integrations.terminal;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/** 终端管理器 - 创建/重用终端 - 通过 runCommand() 运行命令，返回 TerminalProcess - 处理 shell 集成事件 */
@Slf4j
public class TerminalManager {
    private final Set<Integer> terminalIds = new HashSet<>();
    private final Map<Integer, TerminalProcess> processes = new HashMap<>();
    private final List<AutoCloseable> disposables = new ArrayList<>();
    private long shellIntegrationTimeout = 4000;
    private boolean terminalReuseEnabled = true;
    private int terminalOutputLineLimit = 500;
    private int subagentTerminalOutputLineLimit = 2000;
    private String defaultTerminalProfile = "default";
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    /**
     * 运行命令
     *
     * @return TerminalProcessResultPromise，可以同时作为 TerminalProcess 和 CompletableFuture 使用
     */
    public TerminalProcessResultPromise runCommand(TerminalInfo terminalInfo, String command) {
        return runCommand(terminalInfo, command, null);
    }

    /**
     * 运行命令（带监听器配置）
     *
     * @param listeners 监听器配置，可以为 null
     * @return TerminalProcessResultPromise，可以同时作为 TerminalProcess 和 CompletableFuture 使用
     */
    public TerminalProcessResultPromise runCommand(
            TerminalInfo terminalInfo, String command, TerminalProcessListeners listeners) {
        log.info(
                "[TerminalManager] Running command on terminal {}: \"{}\"",
                terminalInfo.getId(),
                command);
        log.info(
                "[TerminalManager] Terminal {} busy state before: {}",
                terminalInfo.getId(),
                terminalInfo.isBusy());

        terminalInfo.setBusy(true);
        terminalInfo.setLastCommand(command);
        TerminalProcess process = new TerminalProcess();
        processes.put(terminalInfo.getId(), process);

        // Register listeners BEFORE calling process.run() to avoid timing issues
        if (listeners != null) {
            if (listeners.getOnLine() != null) {
                process.onLine(listeners.getOnLine());
            }
            if (listeners.getOnCompleted() != null) {
                process.onceCompleted(listeners.getOnCompleted());
            }
            if (listeners.getOnError() != null) {
                process.onceError(error -> listeners.getOnError().run());
            }
            if (listeners.getOnNoShellIntegration() != null) {
                process.onceNoShellIntegration(listeners.getOnNoShellIntegration());
            }
        }

        // Internal listeners (always registered)
        process.onceCompleted(
                () -> {
                    log.info(
                            "[TerminalManager] Terminal {} completed, setting busy to false",
                            terminalInfo.getId());
                    terminalInfo.setBusy(false);
                });

        process.onceNoShellIntegration(
                () -> {
                    log.info("no_shell_integration received for terminal {}", terminalInfo.getId());
                    TerminalRegistry.removeTerminal(terminalInfo.getId());
                    terminalIds.remove(terminalInfo.getId());
                    processes.remove(terminalInfo.getId());
                });

        CompletableFuture<Void> promise = new CompletableFuture<>();
        process.onceContinue(() -> promise.complete(null));
        process.onceError(
                error -> {
                    log.error(
                            "Error in terminal {}: {}",
                            terminalInfo.getId(),
                            error.getMessage(),
                            error);
                    promise.completeExceptionally(error);
                });

        Terminal.ShellIntegration shellIntegration =
                terminalInfo.getTerminal().getShellIntegration();
        if (shellIntegration != null && shellIntegration.getCwd() != null) {
            // Shell integration already active, run command immediately
            // Promise will be resolved via the 'continue' event listener registered above
            // Also add a fallback: if process.run() completes but continue event doesn't fire,
            // resolve promise
            process.run(terminalInfo.getTerminal(), command)
                    .thenRun(
                            () -> {
                                // Fallback: resolve promise if continue event didn't fire
                                if (!promise.isDone()) {
                                    promise.complete(null);
                                }
                            })
                    .exceptionally(
                            error -> {
                                // Fallback: reject promise if continue event didn't fire
                                if (!promise.isDone()) {
                                    promise.completeExceptionally(error);
                                }
                                return null;
                            });
        } else {
            // Wait for shell integration to activate (polling check)
            log.info(
                    "[TerminalManager Test] Waiting for shell integration for terminal {} with timeout {}ms",
                    terminalInfo.getId(),
                    shellIntegrationTimeout);

            CompletableFuture<Void> waitForShellIntegration = new CompletableFuture<>();
            CompletableFuture<Void> timeoutFuture = new CompletableFuture<>();

            // Poll for shell integration activation (check every 100ms)
            ScheduledFuture<?> pollTask =
                    scheduler.scheduleAtFixedRate(
                            () -> {
                                Terminal.ShellIntegration currentShellIntegration =
                                        terminalInfo.getTerminal().getShellIntegration();
                                if (currentShellIntegration != null
                                        && currentShellIntegration.getCwd() != null) {
                                    if (!waitForShellIntegration.isDone()) {
                                        log.info(
                                                "[TerminalManager Test] Shell integration activated for terminal {} within timeout.",
                                                terminalInfo.getId());
                                        waitForShellIntegration.complete(null);
                                    }
                                }
                            },
                            0,
                            100,
                            TimeUnit.MILLISECONDS);

            // Set timeout
            scheduler.schedule(
                    () -> {
                        if (!waitForShellIntegration.isDone()) {
                            log.warn(
                                    "[TerminalManager Test] Shell integration timed out or failed for terminal {}.",
                                    terminalInfo.getId());
                            timeoutFuture.complete(null);
                        }
                    },
                    shellIntegrationTimeout,
                    TimeUnit.MILLISECONDS);

            // Wait for either shell integration activation or timeout
            CompletableFuture.anyOf(waitForShellIntegration, timeoutFuture)
                    .thenRun(
                            () -> {
                                pollTask.cancel(false);
                                log.info(
                                        "[TerminalManager Test] Proceeding with command execution for terminal {}.",
                                        terminalInfo.getId());
                                TerminalProcess existingProcess =
                                        processes.get(terminalInfo.getId());
                                if (existingProcess != null) {
                                    // Promise will be resolved via the 'continue' event listener
                                    // registered above
                                    // Also add a fallback: if process.run() completes but continue
                                    // event doesn't fire, resolve promise
                                    existingProcess
                                            .run(terminalInfo.getTerminal(), command)
                                            .thenRun(
                                                    () -> {
                                                        // Fallback: resolve promise if continue
                                                        // event didn't fire
                                                        if (!promise.isDone()) {
                                                            promise.complete(null);
                                                        }
                                                    })
                                            .exceptionally(
                                                    error -> {
                                                        // Fallback: reject promise if continue
                                                        // event didn't fire
                                                        if (!promise.isDone()) {
                                                            promise.completeExceptionally(error);
                                                        }
                                                        return null;
                                                    });
                                }
                            });
        }

        return new TerminalProcessResultPromise(process, promise);
    }

    /** 获取或创建终端 */
    public CompletableFuture<TerminalInfo> getOrCreateTerminal(String cwd) {
        List<TerminalInfo> terminals = TerminalRegistry.getAllTerminals();
        String expectedShellPath =
                !"default".equals(defaultTerminalProfile)
                        ? getShellForProfile(defaultTerminalProfile)
                        : null;

        log.info("[TerminalManager] Looking for terminal in cwd: {}", cwd);
        log.info("[TerminalManager] Available terminals: {}", terminals.size());

        TerminalInfo matchingTerminal =
                terminals.stream()
                        .filter(
                                t -> {
                                    if (t.isBusy()) {
                                        log.info(
                                                "[TerminalManager] Terminal {} is busy, skipping",
                                                t.getId());
                                        return false;
                                    }
                                    if (!Objects.equals(t.getShellPath(), expectedShellPath)) {
                                        return false;
                                    }
                                    Path terminalCwd =
                                            t.getTerminal().getShellIntegration() != null
                                                    ? t.getTerminal().getShellIntegration().getCwd()
                                                    : null;
                                    if (terminalCwd == null) {
                                        log.info(
                                                "[TerminalManager] Terminal {} has no cwd, skipping",
                                                t.getId());
                                        return false;
                                    }
                                    boolean matches = arePathsEqual(cwd, terminalCwd.toString());
                                    log.info(
                                            "[TerminalManager] Terminal {} cwd: {}, matches: {}",
                                            t.getId(),
                                            terminalCwd,
                                            matches);
                                    return matches;
                                })
                        .findFirst()
                        .orElse(null);

        if (matchingTerminal != null) {
            log.info(
                    "[TerminalManager] Found matching terminal {} in correct cwd",
                    matchingTerminal.getId());
            terminalIds.add(matchingTerminal.getId());
            return CompletableFuture.completedFuture(matchingTerminal);
        }

        if (terminalReuseEnabled) {
            TerminalInfo availableTerminal =
                    terminals.stream()
                            .filter(
                                    t ->
                                            !t.isBusy()
                                                    && Objects.equals(
                                                            t.getShellPath(), expectedShellPath))
                            .findFirst()
                            .orElse(null);

            if (availableTerminal != null) {
                CompletableFuture<Void> cwdPromise = new CompletableFuture<>();
                availableTerminal.setPendingCwdChange(cwd);
                availableTerminal.setCwdResolved(new TerminalInfo.CwdResolved(cwdPromise));

                TerminalProcessResultPromise cdProcessResult =
                        runCommand(availableTerminal, "cd \"" + cwd + "\"");
                CompletableFuture<Void> cdProcess = cdProcessResult.getPromise();

                return cdProcess.thenCompose(
                        result -> {
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }

                            if (isCwdMatchingExpected(availableTerminal)) {
                                if (availableTerminal.getCwdResolved() != null) {
                                    availableTerminal.getCwdResolved().resolve();
                                }
                                availableTerminal.setPendingCwdChange(null);
                                availableTerminal.setCwdResolved(null);
                                terminalIds.add(availableTerminal.getId());
                                return CompletableFuture.completedFuture(availableTerminal);
                            } else {
                                CompletableFuture<Void> timeoutFuture = new CompletableFuture<>();
                                scheduler.schedule(
                                        () -> {
                                            if (!cwdPromise.isDone()) {
                                                timeoutFuture.completeExceptionally(
                                                        new RuntimeException(
                                                                "CWD timeout: Failed to update to "
                                                                        + cwd));
                                            }
                                        },
                                        1,
                                        TimeUnit.SECONDS);

                                return CompletableFuture.anyOf(cwdPromise, timeoutFuture)
                                        .thenApply(
                                                unused -> {
                                                    availableTerminal.setPendingCwdChange(null);
                                                    availableTerminal.setCwdResolved(null);
                                                    terminalIds.add(availableTerminal.getId());
                                                    return availableTerminal;
                                                })
                                        .exceptionally(
                                                error -> {
                                                    availableTerminal.setPendingCwdChange(null);
                                                    availableTerminal.setCwdResolved(null);
                                                    terminalIds.add(availableTerminal.getId());
                                                    return availableTerminal;
                                                });
                            }
                        });
            }
        }

        TerminalInfo newTerminalInfo =
                TerminalRegistry.createTerminal(Paths.get(cwd), expectedShellPath);
        terminalIds.add(newTerminalInfo.getId());
        return CompletableFuture.completedFuture(newTerminalInfo);
    }

    /** 获取终端列表 */
    public List<TerminalSummary> getTerminals(boolean busy) {
        return terminalIds.stream()
                .map(id -> TerminalRegistry.getTerminal(id.intValue()))
                .filter(Objects::nonNull)
                .filter(t -> t.isBusy() == busy)
                .map(t -> new TerminalSummary(t.getId(), t.getLastCommand()))
                .collect(Collectors.toList());
    }

    @Data
    public static class TerminalSummary {
        private final int id;
        private final String lastCommand;
    }

    /** 获取未检索的输出 */
    public String getUnretrievedOutput(int terminalId) {
        if (!terminalIds.contains(terminalId)) {
            return "";
        }
        TerminalProcess process = processes.get(terminalId);
        return process != null ? process.getUnretrievedOutput() : "";
    }

    /** 检查进程是否处于热状态 */
    public boolean isProcessHot(int terminalId) {
        TerminalProcess process = processes.get(terminalId);
        return process != null && process.isHot();
    }

    /** 释放所有资源 */
    public void disposeAll() {
        terminalIds.clear();
        processes.values().forEach(TerminalProcess::shutdown);
        processes.clear();
        disposables.forEach(
                disposable -> {
                    try {
                        disposable.close();
                    } catch (Exception e) {
                        log.error("Error disposing resource", e);
                    }
                });
        disposables.clear();
        scheduler.shutdown();
    }

    /** 关闭资源（实现 Closeable 接口） */
    public void close() {
        disposeAll();
    }

    /** 获取 Shell 集成超时时间 */
    public long getShellIntegrationTimeout() {
        return shellIntegrationTimeout;
    }

    /** 获取终端重用是否启用 */
    public boolean isTerminalReuseEnabled() {
        return terminalReuseEnabled;
    }

    /** 获取终端输出行数限制 */
    public int getTerminalOutputLineLimit() {
        return terminalOutputLineLimit;
    }

    /** 获取子代理终端输出行数限制 */
    public int getSubagentTerminalOutputLineLimit() {
        return subagentTerminalOutputLineLimit;
    }

    /** 设置 Shell 集成超时时间 */
    public void setShellIntegrationTimeout(long timeout) {
        this.shellIntegrationTimeout = timeout;
    }

    /** 设置终端重用是否启用 */
    public void setTerminalReuseEnabled(boolean enabled) {
        this.terminalReuseEnabled = enabled;
    }

    /** 设置终端输出行数限制 */
    public void setTerminalOutputLineLimit(int limit) {
        this.terminalOutputLineLimit = limit;
    }

    /** 设置子代理终端输出行数限制 */
    public void setSubagentTerminalOutputLineLimit(int limit) {
        this.subagentTerminalOutputLineLimit = limit;
    }

    /** 处理输出 */
    public String processOutput(
            List<String> outputLines, Integer overrideLimit, Boolean isSubagentCommand) {
        int limit =
                Boolean.TRUE.equals(isSubagentCommand)
                        ? (overrideLimit != null ? overrideLimit : subagentTerminalOutputLineLimit)
                        : terminalOutputLineLimit;
        if (outputLines.size() > limit) {
            int halfLimit = limit / 2;
            List<String> start = outputLines.subList(0, halfLimit);
            List<String> end =
                    outputLines.subList(outputLines.size() - halfLimit, outputLines.size());
            return (String.join("\n", start)
                            + "\n... (output truncated) ...\n"
                            + String.join("\n", end))
                    .trim();
        }
        return String.join("\n", outputLines).trim();
    }

    /** 设置默认终端配置文件 */
    public ProfileChangeResult setDefaultTerminalProfile(String profileId) {
        if (Objects.equals(defaultTerminalProfile, profileId)) {
            return new ProfileChangeResult(0, Collections.emptyList());
        }

        defaultTerminalProfile = profileId;

        String newShellPath = !"default".equals(profileId) ? getShellForProfile(profileId) : null;
        ProfileChangeResult result = handleTerminalProfileChange(newShellPath);

        List<TerminalInfo> allTerminals = TerminalRegistry.getAllTerminals();
        allTerminals.forEach(
                terminal -> {
                    if (!Objects.equals(terminal.getShellPath(), newShellPath)) {
                        TerminalInfo updates = new TerminalInfo();
                        updates.setLastActive(System.currentTimeMillis());
                        TerminalRegistry.updateTerminal(terminal.getId(), updates);
                    }
                });

        return result;
    }

    @Data
    public static class ProfileChangeResult {
        private final int closedCount;
        private final List<TerminalInfo> busyTerminals;
    }

    /** 过滤终端 */
    public List<TerminalInfo> filterTerminals(Predicate<TerminalInfo> filterFn) {
        return TerminalRegistry.getAllTerminals().stream()
                .filter(filterFn)
                .collect(Collectors.toList());
    }

    /** 关闭终端 */
    public int closeTerminals(Predicate<TerminalInfo> filterFn, boolean force) {
        List<TerminalInfo> terminalsToClose = filterTerminals(filterFn);
        int closedCount = 0;

        for (TerminalInfo terminalInfo : terminalsToClose) {
            if (terminalInfo.isBusy() && !force) {
                continue;
            }

            if (terminalIds.contains(terminalInfo.getId())) {
                terminalIds.remove(terminalInfo.getId());
            }
            TerminalProcess process = processes.remove(terminalInfo.getId());
            if (process != null) {
                process.shutdown();
            }

            terminalInfo.getTerminal().dispose();
            TerminalRegistry.removeTerminal(terminalInfo.getId());
            closedCount++;
        }

        return closedCount;
    }

    /** 处理终端配置文件变更 */
    private ProfileChangeResult handleTerminalProfileChange(String newShellPath) {
        int closedCount =
                closeTerminals(
                        terminal ->
                                !terminal.isBusy()
                                        && !Objects.equals(terminal.getShellPath(), newShellPath),
                        false);

        List<TerminalInfo> busyTerminals =
                filterTerminals(
                        terminal ->
                                terminal.isBusy()
                                        && !Objects.equals(terminal.getShellPath(), newShellPath));

        return new ProfileChangeResult(closedCount, busyTerminals);
    }

    /** 关闭所有终端 */
    public int closeAllTerminals() {
        return closeTerminals(t -> true, true);
    }

    /** 检查 CWD 是否匹配预期 */
    private boolean isCwdMatchingExpected(TerminalInfo terminalInfo) {
        if (terminalInfo.getPendingCwdChange() == null) {
            return false;
        }

        Path currentCwd =
                terminalInfo.getTerminal().getShellIntegration() != null
                        ? terminalInfo.getTerminal().getShellIntegration().getCwd()
                        : null;

        if (currentCwd == null) {
            return false;
        }

        return arePathsEqual(currentCwd.toString(), terminalInfo.getPendingCwdChange());
    }

    /** 比较两个路径是否相等 */
    private static boolean arePathsEqual(String path1, String path2) {
        if (path1 == null && path2 == null) {
            return true;
        }
        if (path1 == null || path2 == null) {
            return false;
        }
        try {
            Path p1 = Paths.get(path1).normalize().toAbsolutePath();
            Path p2 = Paths.get(path2).normalize().toAbsolutePath();
            return p1.equals(p2);
        } catch (Exception e) {
            return path1.equals(path2);
        }
    }

    /** 根据配置文件获取 Shell 路径 支持多种 shell 类型，提供回退逻辑 */
    private String getShellForProfile(String profileId) {
        String os = System.getProperty("os.name").toLowerCase();
        String osName = os.toLowerCase();

        // 如果 profileId 是 "default"，使用默认 shell
        if ("default".equals(profileId)) {
            return getDefaultShell(osName);
        }

        // 根据 profileId 返回对应的 shell 路径
        switch (profileId.toLowerCase()) {
            case "powershell-7":
            case "pwsh":
                return "C:\\Program Files\\PowerShell\\7\\pwsh.exe";
            case "powershell-legacy":
            case "powershell":
                return "C:\\Windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe";
            case "cmd":
                return "C:\\Windows\\System32\\cmd.exe";
            case "wsl-bash":
            case "wsl":
                return "/bin/bash";
            case "git-bash":
                return "C:\\Program Files\\Git\\bin\\bash.exe";
            case "bash":
                return "/bin/bash";
            case "zsh":
                return "/bin/zsh";
            case "sh":
                return "/bin/sh";
            case "csh":
                return "/bin/csh";
            case "ksh":
                return "/bin/ksh";
            case "dash":
                return "/bin/dash";
            case "tcsh":
                return "/bin/tcsh";
            default:
                // 如果 profileId 不匹配，回退到默认 shell
                return getDefaultShell(osName);
        }
    }

    /** 获取默认 shell（根据操作系统） */
    private String getDefaultShell(String osName) {
        if (osName.contains("win")) {
            // Windows: 尝试从环境变量获取，否则使用 cmd
            String comspec = System.getenv("COMSPEC");
            if (comspec != null && !comspec.isEmpty()) {
                return comspec;
            }
            return "C:\\Windows\\System32\\cmd.exe";
        } else if (osName.contains("mac") || osName.contains("darwin")) {
            // macOS: 尝试从环境变量获取，否则使用 zsh
            String shell = System.getenv("SHELL");
            if (shell != null && !shell.isEmpty()) {
                return shell;
            }
            return "/bin/zsh";
        } else {
            // Linux/Unix: 尝试从环境变量获取，否则使用 bash
            String shell = System.getenv("SHELL");
            if (shell != null && !shell.isEmpty()) {
                return shell;
            }
            return "/bin/bash";
        }
    }
}
