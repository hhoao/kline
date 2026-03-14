package com.hhoa.kline.core.core.integrations.terminal;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.extern.slf4j.Slf4j;

/** 基于 ProcessBuilder 的终端实现 */
@Slf4j
public class ProcessTerminal implements Terminal {
    private final Path cwd;
    private final String shellPath;
    private Process process;
    private Integer exitStatus;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private Path currentCwd;

    public ProcessTerminal(Path cwd, String shellPath) {
        this.cwd = cwd;
        this.shellPath = shellPath;
        this.currentCwd = cwd;
    }

    @Override
    public Path getCwd() {
        return currentCwd != null ? currentCwd : cwd;
    }

    @Override
    public ShellIntegration getShellIntegration() {
        return new ProcessShellIntegration();
    }

    @Override
    public void sendText(String text, boolean addNewLine) {
        if (addNewLine && !text.endsWith("\n")) {
            text += "\n";
        }
        try {
            if (process != null && process.isAlive()) {
                process.getOutputStream().write(text.getBytes());
                process.getOutputStream().flush();
            }
        } catch (IOException e) {
            log.error("Failed to send text to terminal", e);
        }
    }

    @Override
    public void show() {
        // 在 Java 环境中，show 操作可能不需要实现
    }

    @Override
    public void dispose() {
        if (process != null && process.isAlive()) {
            process.destroyForcibly();
        }
        executor.shutdown();
    }

    @Override
    public Integer getExitStatus() {
        if (process != null && !process.isAlive()) {
            try {
                return process.exitValue();
            } catch (IllegalThreadStateException e) {
                return null;
            }
        }
        return exitStatus;
    }

    private class ProcessShellIntegration implements ShellIntegration {
        @Override
        public Path getCwd() {
            return ProcessTerminal.this.getCwd();
        }

        @Override
        public CommandExecution executeCommand(String command) {
            return new ProcessCommandExecution(command);
        }
    }

    private class ProcessCommandExecution implements CommandExecution {
        private final String command;

        public ProcessCommandExecution(String command) {
            this.command = command;
        }

        @Override
        public CompletableFuture<Void> read(java.util.function.Consumer<String> onData) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            executor.submit(
                    () -> {
                        try {
                            ProcessBuilder pb = new ProcessBuilder();
                            if (shellPath != null) {
                                pb.command(shellPath, "-c", command);
                            } else {
                                String os = System.getProperty("os.name").toLowerCase();
                                if (os.contains("win")) {
                                    pb.command("cmd.exe", "/c", command);
                                } else {
                                    pb.command("sh", "-c", command);
                                }
                            }
                            pb.directory(new File(getCwd().toString()));
                            pb.redirectErrorStream(true);
                            Process p = pb.start();
                            process = p;

                            byte[] buffer = new byte[1024];
                            int bytesRead;
                            while ((bytesRead = p.getInputStream().read(buffer)) != -1) {
                                String chunk = new String(buffer, 0, bytesRead);
                                onData.accept(chunk);
                            }

                            p.waitFor();
                            exitStatus = p.exitValue();
                            future.complete(null);
                        } catch (Exception e) {
                            log.error("Failed to execute command: " + command, e);
                            future.completeExceptionally(e);
                        }
                    });
            return future;
        }
    }
}
