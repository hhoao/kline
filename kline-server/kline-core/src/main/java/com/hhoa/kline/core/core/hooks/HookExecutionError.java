package com.hhoa.kline.core.core.hooks;

import lombok.Getter;

/** Hook 执行期间抛出的带有结构化信息的异常 */
@Getter
public class HookExecutionError extends RuntimeException {

    private final HookErrorInfo errorInfo;

    public HookExecutionError(HookErrorInfo errorInfo, String message) {
        super(message != null ? message : errorInfo.getMessage());
        this.errorInfo = errorInfo;
    }

    public HookExecutionError(HookErrorInfo errorInfo) {
        this(errorInfo, null);
    }

    public static boolean isHookError(Throwable error) {
        return error instanceof HookExecutionError;
    }

    public static HookExecutionError timeout(
            String scriptPath, long timeoutMs, String stderr, String hookName) {
        String hookPrefix = hookName != null ? hookName + " hook" : "Hook";
        return new HookExecutionError(
                HookErrorInfo.builder()
                        .type(HookErrorType.TIMEOUT)
                        .message(hookPrefix + " execution timed out after " + timeoutMs + "ms")
                        .details(
                                "The hook took longer than "
                                        + (timeoutMs / 1000)
                                        + " seconds to complete.\n\n"
                                        + "Common causes:\n"
                                        + "• Infinite loop in hook script\n"
                                        + "• Network request hanging without timeout\n"
                                        + "• File I/O operation stuck\n"
                                        + "• Heavy computation taking too long\n\n"
                                        + "Recommendations:\n"
                                        + "1. Check your hook script for infinite loops\n"
                                        + "2. Add timeouts to network requests\n"
                                        + "3. Use background jobs for long operations\n"
                                        + "4. Test your hook script independently")
                        .scriptPath(scriptPath)
                        .stderr(stderr)
                        .build());
    }

    public static HookExecutionError validation(
            String validationError, String scriptPath, String stdoutPreview) {
        return new HookExecutionError(
                HookErrorInfo.builder()
                        .type(HookErrorType.VALIDATION)
                        .message("Hook output validation failed")
                        .details(validationError + "\n\nOutput preview:\n" + stdoutPreview)
                        .scriptPath(scriptPath)
                        .build());
    }

    public static HookExecutionError execution(
            String scriptPath, int exitCode, String stderr, String hookName) {
        String hookPrefix = hookName != null ? hookName + " hook" : "Hook script";
        String message = hookPrefix + " exited with code " + exitCode;
        return new HookExecutionError(
                HookErrorInfo.builder()
                        .type(HookErrorType.EXECUTION)
                        .message(message)
                        .details(stderr != null ? "stderr:\n" + stderr : null)
                        .scriptPath(scriptPath)
                        .exitCode(exitCode)
                        .stderr(stderr)
                        .build(),
                message);
    }

    public static HookExecutionError cancellation(String scriptPath, String hookName) {
        String hookPrefix = hookName != null ? hookName + " hook" : "Hook";
        return new HookExecutionError(
                HookErrorInfo.builder()
                        .type(HookErrorType.CANCELLATION)
                        .message(hookPrefix + " execution was cancelled")
                        .details("The hook was cancelled by the user before completion")
                        .scriptPath(scriptPath)
                        .exitCode(130)
                        .build());
    }
}
