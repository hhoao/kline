package com.hhoa.kline.core.core.task.tools.subagent;

import com.hhoa.kline.core.core.assistant.ToolUse;
import com.hhoa.kline.core.core.prompts.ResponseFormatter;
import com.hhoa.kline.core.core.task.TaskState;
import com.hhoa.kline.core.core.task.tools.handlers.ToolHandler;
import com.hhoa.kline.core.core.task.tools.types.ToolContext;
import com.hhoa.kline.core.core.task.tools.types.ToolExecuteResult;
import com.hhoa.kline.core.enums.ClineDefaultTool;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * 与 Cline {@code SubagentRunner.ts} 对齐：运行子代理对话循环，执行工具调用，追踪统计信息。
 *
 * <p>在 Java 服务端环境中，API 交互通过 Spring AI 完成，因此具体的流式调用需要适配。
 */
@Slf4j
public class SubagentRunner {

    private static final int MAX_EMPTY_ASSISTANT_RETRIES = 3;
    private static final int MAX_INITIAL_STREAM_ATTEMPTS = 3;
    private static final long INITIAL_STREAM_RETRY_BASE_DELAY_MS = 2_000L;

    private final SubagentBuilder agent;
    private final ToolContext baseConfig;
    private final List<ClineDefaultTool> allowedTools;
    private final AtomicBoolean abortRequested = new AtomicBoolean(false);
    private final AtomicInteger activeCommandExecutions = new AtomicInteger(0);
    private final ResponseFormatter formatResponse = new ResponseFormatter();

    public SubagentRunner(ToolContext baseConfig, String subagentName) {
        this.baseConfig = baseConfig;
        this.agent =
                new SubagentBuilder(baseConfig, subagentName != null ? subagentName : "subagent");
        this.allowedTools = agent.getAllowedTools();
    }

    public void abort() {
        abortRequested.set(true);
        if (activeCommandExecutions.get() > 0 && baseConfig.getCallbacks() != null) {
            try {
                baseConfig.getCallbacks().cancelRunningCommandTool();
            } catch (Exception e) {
                log.error("[SubagentRunner] failed to cancel running command execution", e);
            }
        }
    }

    private boolean shouldAbort() {
        return abortRequested.get()
                || (baseConfig.getTaskState() != null && baseConfig.getTaskState().isAbort());
    }

    /**
     * 运行子代理：执行对话循环，处理工具调用，直到完成或失败。
     *
     * @param prompt 子代理的初始提示
     * @param onProgress 进度回调
     * @return 运行结果
     */
    public SubagentRunResult run(String prompt, Consumer<SubagentProgressUpdate> onProgress) {
        abortRequested.set(false);
        int emptyAssistantResponseRetries = 0;
        SubagentRunStats stats = new SubagentRunStats();

        onProgress.accept(SubagentProgressUpdate.builder().status("running").stats(stats).build());

        try {
            if (shouldAbort()) {
                abort();
                String error = "Subagent run cancelled.";
                onProgress.accept(
                        SubagentProgressUpdate.builder()
                                .status("failed")
                                .error(error)
                                .stats(stats)
                                .build());
                return SubagentRunResult.failed(error, stats);
            }

            String systemPrompt = agent.buildSystemPrompt(buildDefaultSystemPrompt());

            // 对话历史
            List<Map<String, Object>> conversation = new ArrayList<>();
            Map<String, Object> initialMessage = new HashMap<>();
            initialMessage.put("role", "user");
            initialMessage.put("content", prompt);
            conversation.add(initialMessage);

            // 主对话循环
            while (true) {
                if (shouldAbort()) {
                    abort();
                    String error = "Subagent run cancelled.";
                    onProgress.accept(
                            SubagentProgressUpdate.builder()
                                    .status("failed")
                                    .error(error)
                                    .stats(stats)
                                    .build());
                    return SubagentRunResult.failed(error, stats);
                }

                // TODO: 调用 AI API 获取助手响应
                // 当前返回占位结果 — 实际实现需要对接 Spring AI 的流式 API
                String assistantText = "";
                List<SubagentToolCall> finalizedToolCalls = new ArrayList<>();

                // 解析工具调用（占位 — 需要从实际 API 响应中解析）
                // finalizedToolCalls = parseToolCallsFromResponse(apiResponse);

                if (finalizedToolCalls.isEmpty()) {
                    emptyAssistantResponseRetries++;
                    if (emptyAssistantResponseRetries > MAX_EMPTY_ASSISTANT_RETRIES) {
                        String error = "Subagent did not call attempt_completion.";
                        onProgress.accept(
                                SubagentProgressUpdate.builder()
                                        .status("failed")
                                        .error(error)
                                        .stats(stats)
                                        .build());
                        return SubagentRunResult.failed(error, stats);
                    }

                    // 添加 nudge 消息
                    Map<String, Object> nudgeMessage = new HashMap<>();
                    nudgeMessage.put("role", "user");
                    nudgeMessage.put("content", formatResponse.noToolsUsed());
                    conversation.add(nudgeMessage);
                    continue;
                }
                emptyAssistantResponseRetries = 0;

                // 处理工具调用
                List<Map<String, Object>> toolResultBlocks = new ArrayList<>();
                for (SubagentToolCall call : finalizedToolCalls) {
                    String toolName = call.getName();

                    // attempt_completion 特殊处理
                    if (ClineDefaultTool.ATTEMPT.getValue().equals(toolName)) {
                        String result =
                                call.getParams() != null
                                        ? String.valueOf(call.getParams().get("result"))
                                        : null;
                        if (result == null || result.trim().isEmpty()) {
                            String missingError =
                                    formatResponse.missingToolParameterError("result");
                            addToolResultBlock(toolResultBlocks, toolName, missingError);
                            continue;
                        }

                        stats.toolCalls++;
                        onProgress.accept(SubagentProgressUpdate.builder().stats(stats).build());
                        onProgress.accept(
                                SubagentProgressUpdate.builder()
                                        .status("completed")
                                        .result(result.trim())
                                        .stats(stats)
                                        .build());
                        return SubagentRunResult.completed(result.trim(), stats);
                    }

                    // 检查工具是否允许
                    ClineDefaultTool tool = findTool(toolName);
                    if (tool == null || !allowedTools.contains(tool)) {
                        String denied =
                                formatResponse.toolError(
                                        "Tool '"
                                                + toolName
                                                + "' is not available inside subagent runs.");
                        addToolResultBlock(toolResultBlocks, toolName, denied);
                        continue;
                    }

                    // 执行工具
                    String latestToolCall = formatToolCallPreview(toolName, call.getParams());
                    onProgress.accept(
                            SubagentProgressUpdate.builder()
                                    .latestToolCall(latestToolCall)
                                    .build());

                    ToolContext subagentConfig = createSubagentToolContext();
                    ToolHandler handler =
                            subagentConfig.getCoordinator().getRegistry().getHandler(toolName);
                    String toolResult;

                    if (handler == null) {
                        toolResult =
                                formatResponse.toolError(
                                        "No handler registered for tool '" + toolName + "'.");
                    } else {
                        try {
                            ToolUse toolUseBlock = new ToolUse();
                            toolUseBlock.setName(toolName);
                            toolUseBlock.setParams(call.getParams());
                            toolUseBlock.setPartial(false);

                            ToolExecuteResult execResult =
                                    handler.execute(subagentConfig, toolUseBlock);
                            toolResult = serializeToolResult(execResult);
                        } catch (Exception e) {
                            toolResult = formatResponse.toolError(e.getMessage());
                        }
                    }

                    stats.toolCalls++;
                    onProgress.accept(SubagentProgressUpdate.builder().stats(stats).build());

                    String toolDescription =
                            handler != null
                                    ? handler.getDescription(createToolUseForDescription(toolName))
                                    : "[" + toolName + "]";
                    addToolResultBlock(toolResultBlocks, toolDescription, toolResult);
                }

                // 添加工具结果到对话
                Map<String, Object> toolResultMessage = new HashMap<>();
                toolResultMessage.put("role", "user");
                toolResultMessage.put("content", toolResultBlocks);
                conversation.add(toolResultMessage);
            }
        } catch (Exception error) {
            if (shouldAbort()) {
                String cancelledError = "Subagent run cancelled.";
                onProgress.accept(
                        SubagentProgressUpdate.builder()
                                .status("failed")
                                .error(cancelledError)
                                .stats(stats)
                                .build());
                return SubagentRunResult.failed(cancelledError, stats);
            }

            String errorText =
                    error.getMessage() != null ? error.getMessage() : "Subagent execution failed.";
            log.error("[SubagentRunner] run failed", error);
            onProgress.accept(
                    SubagentProgressUpdate.builder()
                            .status("failed")
                            .error(errorText)
                            .stats(stats)
                            .build());
            return SubagentRunResult.failed(errorText, stats);
        }
    }

    private ToolContext createSubagentToolContext() {
        return ToolContext.builder()
                .taskId(baseConfig.getTaskId())
                .ulid(baseConfig.getUlid())
                .cwd(baseConfig.getCwd())
                .mode(baseConfig.getMode())
                .workspaceManager(baseConfig.getWorkspaceManager())
                .yoloModeToggled(baseConfig.isYoloModeToggled())
                .isSubagentExecution(true)
                .taskState(new TaskState())
                .services(baseConfig.getServices())
                .autoApprovalSettings(baseConfig.getAutoApprovalSettings())
                .callbacks(baseConfig.getCallbacks())
                .coordinator(baseConfig.getCoordinator())
                .build();
    }

    private String buildDefaultSystemPrompt() {
        return "You are a helpful assistant.";
    }

    private String serializeToolResult(ToolExecuteResult result) {
        if (result instanceof ToolExecuteResult.Immediate immediate) {
            if (immediate.blocks() == null || immediate.blocks().isEmpty()) {
                return "(tool did not return anything)";
            }
            StringBuilder sb = new StringBuilder();
            for (var block : immediate.blocks()) {
                if (sb.length() > 0) {
                    sb.append("\n");
                }
                sb.append(block.toString());
            }
            return sb.toString();
        }
        return "(tool returned non-immediate result)";
    }

    private void addToolResultBlock(
            List<Map<String, Object>> blocks, String label, String content) {
        Map<String, Object> block = new HashMap<>();
        block.put("type", "text");
        block.put("text", label + " Result:\n" + content);
        blocks.add(block);
    }

    private ToolUse createToolUseForDescription(String toolName) {
        ToolUse toolUse = new ToolUse();
        toolUse.setName(toolName);
        return toolUse;
    }

    private static String formatToolCallPreview(String toolName, Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return toolName + "()";
        }
        List<String> entries = new ArrayList<>();
        int count = 0;
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            if (count >= 3) {
                entries.add("...+" + (params.size() - 3));
                break;
            }
            String value = String.valueOf(entry.getValue());
            String normalized = value.replaceAll("\\s+", " ").trim();
            if (normalized.length() > 48) {
                normalized = normalized.substring(0, 45) + "...";
            }
            entries.add(entry.getKey() + "=" + normalized);
            count++;
        }
        return toolName + "(" + String.join(", ", entries) + ")";
    }

    private static ClineDefaultTool findTool(String toolName) {
        if (toolName == null) {
            return null;
        }
        for (ClineDefaultTool tool : ClineDefaultTool.values()) {
            if (tool.getValue().equals(toolName)) {
                return tool;
            }
        }
        return null;
    }

    // ─── Inner types ───────────────────────────────────────────────────

    @Data
    public static class SubagentRunResult {
        private final String status; // "completed" | "failed"
        private final String result;
        private final String error;
        private final SubagentRunStats stats;

        public static SubagentRunResult completed(String result, SubagentRunStats stats) {
            return new SubagentRunResult("completed", result, null, stats);
        }

        public static SubagentRunResult failed(String error, SubagentRunStats stats) {
            return new SubagentRunResult("failed", null, error, stats);
        }
    }

    @Data
    @Builder
    public static class SubagentProgressUpdate {
        private SubagentRunStats stats;
        private String latestToolCall;
        private String status; // "running" | "completed" | "failed"
        private String result;
        private String error;
    }

    @Data
    public static class SubagentRunStats {
        private int toolCalls;
        private int inputTokens;
        private int outputTokens;
        private int cacheWriteTokens;
        private int cacheReadTokens;
        private double totalCost;
        private int contextTokens;
        private int contextWindow;
        private double contextUsagePercentage;
    }

    @Data
    public static class SubagentToolCall {
        private String toolUseId;
        private String name;
        private Map<String, Object> params;
        private String callId;
        private String signature;
        private boolean nativeToolCall;
    }
}
