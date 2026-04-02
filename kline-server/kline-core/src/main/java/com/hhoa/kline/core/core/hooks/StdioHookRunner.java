package com.hhoa.kline.core.core.hooks;

import com.fasterxml.jackson.core.type.TypeReference;
import com.hhoa.ai.kline.commons.utils.JsonUtils;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * 通过子进程执行 hook 脚本，支持实时输出流。
 *
 * <p>功能：
 *
 * <ul>
 *   <li>将 HookInput 序列化为 JSON 通过 stdin 传递给脚本
 *   <li>通过回调逐行流式输出 stdout/stderr
 *   <li>强制 30 秒超时
 *   <li>从 stdout 解析 JSON 响应
 *   <li>截断超过 50KB 的 contextModification
 *   <li>"Fail-open" 设计：只有显式 cancel:true 才阻止工具执行
 * </ul>
 */
@Slf4j
public class StdioHookRunner extends HookRunner {

    private static final long HOOK_EXECUTION_TIMEOUT_MS = 30000;
    private static final int MAX_CONTEXT_MODIFICATION_SIZE = 50000;

    private final String scriptPath;
    private final String source; // "global" or "workspace"
    private final HookStreamCallback streamCallback;
    private final String cwd;

    public StdioHookRunner(
            HookName hookName,
            String scriptPath,
            String source,
            HookStreamCallback streamCallback,
            String cwd) {
        super(hookName);
        this.scriptPath = scriptPath;
        this.source = source;
        this.streamCallback = streamCallback;
        this.cwd = cwd;
    }

    @Override
    public HookOutput execute(HookInput input) {
        String inputJson = JsonUtils.toJsonString(input);

        HookProcess hookProcess = new HookProcess(scriptPath, HOOK_EXECUTION_TIMEOUT_MS, cwd);

        // 设置流式回调
        if (streamCallback != null) {
            hookProcess.onLine(
                    (line, stream) -> streamCallback.onLine(line, stream, source, scriptPath));
        }

        // 执行 hook 脚本
        hookProcess.run(inputJson);

        String stdout = hookProcess.getStdoutString();
        String stderr = hookProcess.getStderrString();
        Integer exitCode = hookProcess.getExitCode();

        // 尝试解析 JSON 输出
        HookOutput parsedOutput = parseJsonOutput(stdout);

        if (parsedOutput != null) {
            // 有有效 JSON，无论退出码如何都使用它
            if (exitCode != null && exitCode != 0) {
                log.warn(
                        "[Hook {}] Exited with code {} but provided valid JSON response",
                        getHookName(),
                        exitCode);
            }
            return parsedOutput;
        }

        // 没有有效 JSON
        if (exitCode != null && exitCode == 0) {
            // Hook 成功但没有 JSON 响应 — 允许执行
            log.warn("[Hook {}] Completed successfully but no JSON response found", getHookName());
            return HookOutput.success();
        }

        // Hook 失败且无有效 JSON
        throw HookExecutionError.execution(
                scriptPath, exitCode != null ? exitCode : 1, stderr, getHookName().getValue());
    }

    /** 尝试从 stdout 解析 JSON，如果直接解析失败，尝试从末尾提取最后一个完整的 JSON 对象 */
    private HookOutput parseJsonOutput(String stdout) {
        if (stdout == null || stdout.trim().isEmpty()) {
            return null;
        }

        // 尝试直接解析
        try {
            Map<String, Object> outputData =
                    JsonUtils.parseObjectQuietly(stdout.trim(), new TypeReference<>() {});
            if (outputData != null && validateHookOutput(outputData)) {
                return buildHookOutput(outputData);
            }
        } catch (Exception ignored) {
            // 直接解析失败，尝试提取
        }

        // 从末尾扫描提取最后一个完整的 JSON 对象
        String jsonCandidate = extractLastJsonObject(stdout);
        if (jsonCandidate != null) {
            try {
                Map<String, Object> outputData =
                        JsonUtils.parseObjectQuietly(jsonCandidate, new TypeReference<>() {});
                if (outputData != null && validateHookOutput(outputData)) {
                    return buildHookOutput(outputData);
                }
            } catch (Exception ignored) {
                // 提取的 JSON 也无效
            }
        }

        return null;
    }

    private boolean validateHookOutput(Map<String, Object> output) {
        // 检查已废弃的 shouldContinue 字段
        if (output.containsKey("shouldContinue")) {
            return false;
        }

        // cancel 如果存在必须是 boolean
        Object cancel = output.get("cancel");
        if (cancel != null && !(cancel instanceof Boolean)) {
            return false;
        }

        // contextModification 如果存在必须是 string
        Object contextMod = output.get("contextModification");
        if (contextMod != null && !(contextMod instanceof String)) {
            return false;
        }

        // errorMessage 如果存在必须是 string
        Object errorMsg = output.get("errorMessage");
        if (errorMsg != null && !(errorMsg instanceof String)) {
            return false;
        }

        return true;
    }

    private HookOutput buildHookOutput(Map<String, Object> data) {
        HookOutput.HookOutputBuilder builder = HookOutput.builder();

        Object cancel = data.get("cancel");
        if (cancel instanceof Boolean) {
            builder.cancel((Boolean) cancel);
        }

        Object contextMod = data.get("contextModification");
        if (contextMod instanceof String) {
            String ctx = (String) contextMod;
            if (ctx.length() > MAX_CONTEXT_MODIFICATION_SIZE) {
                log.warn(
                        "Hook {} returned contextModification of {} bytes, truncating to {} bytes",
                        getHookName(),
                        ctx.length(),
                        MAX_CONTEXT_MODIFICATION_SIZE);
                ctx =
                        ctx.substring(0, MAX_CONTEXT_MODIFICATION_SIZE)
                                + "\n\n[... context truncated due to size limit ...]";
            }
            builder.contextModification(ctx);
        }

        Object errorMsg = data.get("errorMessage");
        if (errorMsg instanceof String) {
            builder.errorMessage((String) errorMsg);
        }

        return builder.build();
    }

    /** 从输出末尾扫描提取最后一个完整的 JSON 对象 */
    private String extractLastJsonObject(String text) {
        String[] lines = text.split("\n");
        StringBuilder jsonCandidate = new StringBuilder();
        int braceCount = 0;
        boolean startCollecting = false;

        for (int i = lines.length - 1; i >= 0; i--) {
            String line = lines[i].stripTrailing();

            for (int j = line.length() - 1; j >= 0; j--) {
                char c = line.charAt(j);
                if (c == '}') {
                    braceCount++;
                    if (!startCollecting) {
                        startCollecting = true;
                    }
                } else if (c == '{') {
                    braceCount--;
                }
            }

            if (startCollecting) {
                jsonCandidate.insert(0, line + "\n");
            }

            if (startCollecting && braceCount == 0) {
                break;
            }
        }

        String result = jsonCandidate.toString().trim();
        if (result.isEmpty()) {
            return null;
        }

        // 去掉第一个 { 之前的内容
        int firstBrace = result.indexOf('{');
        return firstBrace >= 0 ? result.substring(firstBrace) : result;
    }
}
