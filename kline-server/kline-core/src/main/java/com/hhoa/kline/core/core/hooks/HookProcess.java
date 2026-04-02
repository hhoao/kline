package com.hhoa.kline.core.core.hooks;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * 管理 hook 脚本的子进程执行。
 *
 * <p>功能：
 *
 * <ul>
 *   <li>通过 stdin/stdout/stderr 与子进程通信
 *   <li>逐行流式输出，支持实时 UI 更新
 *   <li>强制 30 秒超时（可配置）
 *   <li>支持通过中断取消
 *   <li>1MB 输出大小限制
 * </ul>
 */
@Slf4j
public class HookProcess {

    private static final int MAX_OUTPUT_SIZE = 1024 * 1024; // 1MB

    private final String scriptPath;
    private final long timeoutMs;
    private final String cwd;

    @Getter private final StringBuilder stdout = new StringBuilder();
    @Getter private final StringBuilder stderr = new StringBuilder();
    @Getter private Integer exitCode;

    /** 逐行输出回调：(line, stream) 其中 stream 为 "stdout" 或 "stderr" */
    private final List<BiConsumer<String, String>> lineListeners = new ArrayList<>();

    public HookProcess(String scriptPath, long timeoutMs, String cwd) {
        this.scriptPath = scriptPath;
        this.timeoutMs = timeoutMs;
        this.cwd = cwd;
    }

    /** 注册行输出监听器 */
    public void onLine(BiConsumer<String, String> listener) {
        lineListeners.add(listener);
    }

    /**
     * 执行 hook 脚本，通过 stdin 传入 JSON 输入
     *
     * @param inputJson 序列化的 HookInput JSON
     * @throws HookExecutionError 超时、取消或执行失败时
     */
    public void run(String inputJson) throws HookExecutionError {
        ProcessBuilder pb = buildProcessBuilder();
        if (cwd != null) {
            pb.directory(new java.io.File(cwd));
        }

        Process process = null;
        try {
            process = pb.start();
            HookProcessRegistry.register(process);

            // 写入 stdin
            try (OutputStream os = process.getOutputStream()) {
                os.write(inputJson.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            // 发出空行作为 "输出开始" 标记
            emitLine("", "stdout");

            // 并行读取 stdout 和 stderr
            Thread stdoutThread = readStreamAsync(process.getInputStream(), "stdout", stdout);
            Thread stderrThread = readStreamAsync(process.getErrorStream(), "stderr", stderr);

            boolean finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);

            stdoutThread.join(2000);
            stderrThread.join(2000);

            if (!finished) {
                process.destroyForcibly();
                throw HookExecutionError.timeout(scriptPath, timeoutMs, stderr.toString(), null);
            }

            exitCode = process.exitValue();
        } catch (HookExecutionError e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            if (process != null) {
                process.destroyForcibly();
            }
            throw HookExecutionError.cancellation(scriptPath, null);
        } catch (IOException e) {
            throw HookExecutionError.execution(scriptPath, -1, e.getMessage(), null);
        } finally {
            if (process != null) {
                HookProcessRegistry.unregister(process);
            }
        }
    }

    private ProcessBuilder buildProcessBuilder() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            return new ProcessBuilder(
                    "powershell",
                    "-NoProfile",
                    "-NonInteractive",
                    "-ExecutionPolicy",
                    "Bypass",
                    "-File",
                    scriptPath);
        } else {
            return new ProcessBuilder(scriptPath);
        }
    }

    private Thread readStreamAsync(
            java.io.InputStream inputStream, String streamName, StringBuilder buffer) {
        Thread thread =
                new Thread(
                        () -> {
                            try (BufferedReader reader =
                                    new BufferedReader(
                                            new InputStreamReader(
                                                    inputStream, StandardCharsets.UTF_8))) {
                                String line;
                                while ((line = reader.readLine()) != null) {
                                    if (buffer.length() < MAX_OUTPUT_SIZE) {
                                        buffer.append(line).append("\n");
                                    }
                                    emitLine(line, streamName);
                                }
                            } catch (IOException e) {
                                log.debug(
                                        "Error reading {} for hook {}", streamName, scriptPath, e);
                            }
                        },
                        "hook-" + streamName + "-reader");
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    private void emitLine(String line, String stream) {
        for (BiConsumer<String, String> listener : lineListeners) {
            try {
                listener.accept(line, stream);
            } catch (Exception e) {
                log.debug("Error in hook line listener", e);
            }
        }
    }

    public String getStdoutString() {
        return stdout.toString();
    }

    public String getStderrString() {
        return stderr.toString();
    }
}
