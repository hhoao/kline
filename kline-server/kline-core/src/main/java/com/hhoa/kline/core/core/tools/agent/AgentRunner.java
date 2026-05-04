package com.hhoa.kline.core.core.tools.agent;

import com.hhoa.kline.core.core.assistant.AssistantMessage;
import com.hhoa.kline.core.core.assistant.AssistantMessageContent;
import com.hhoa.kline.core.core.assistant.MessageParam;
import com.hhoa.kline.core.core.assistant.TextContentBlock;
import com.hhoa.kline.core.core.assistant.ToolUse;
import com.hhoa.kline.core.core.assistant.UserContentBlock;
import com.hhoa.kline.core.core.assistant.UserMessage;
import com.hhoa.kline.core.core.assistant.parser.AssistantMessageParser;
import com.hhoa.kline.core.core.prompts.ResponseFormatter;
import com.hhoa.kline.core.core.task.ApiChunk;
import com.hhoa.kline.core.core.task.TaskState;
import com.hhoa.kline.core.core.tools.ToolHandlerInvocationSupport;
import com.hhoa.kline.core.core.tools.ToolParamCatalog;
import com.hhoa.kline.core.core.tools.handlers.ToolHandler;
import com.hhoa.kline.core.core.tools.types.ToolContext;
import com.hhoa.kline.core.core.tools.types.ToolExecuteResult;
import com.hhoa.kline.core.core.tools.ClineDefaultTool;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import reactor.core.publisher.Flux;

public class AgentRunner {
    private static final int MAX_TURNS = 25;

    private final ToolContext parentContext;
    private final AgentDefinition agentDefinition;
    private final AssistantMessageParser parser;
    private final ResponseFormatter responseFormatter = new ResponseFormatter();

    public AgentRunner(ToolContext parentContext, AgentDefinition agentDefinition) {
        this.parentContext = parentContext;
        this.agentDefinition = agentDefinition;
        this.parser =
                new AssistantMessageParser(
                        new ToolParamCatalog(parentContext.getCoordinator().getRegistry()).all());
    }

    public AgentRunResult run(String prompt) {
        if (parentContext.getApiHandler() == null) {
            return AgentRunResult.failed(
                    "Agent execution requires ToolContext.apiHandler.", AgentRunStats.empty());
        }
        AgentRunStats stats = new AgentRunStats();
        List<MessageParam> conversation = new ArrayList<>();
        conversation.add(userMessage(prompt));

        for (int turn = 0; turn < MAX_TURNS; turn++) {
            String assistantMessage = readAssistantMessage(conversation, stats);
            conversation.add(assistantMessage(assistantMessage));

            List<AssistantMessageContent> blocks = parser.parseAssistantMessage(assistantMessage);
            List<ToolUse> toolUses =
                    blocks.stream()
                            .filter(ToolUse.class::isInstance)
                            .map(ToolUse.class::cast)
                            .filter(tool -> !tool.isPartial())
                            .toList();

            if (toolUses.isEmpty()) {
                conversation.add(userMessage(responseFormatter.noToolsUsed()));
                continue;
            }

            List<String> toolResults = new ArrayList<>();
            for (ToolUse toolUse : toolUses) {
                String toolName = toolUse.getName();
                if (ClineDefaultTool.ATTEMPT.getValue().equals(toolName)) {
                    String result = stringParam(toolUse.getParams(), "result");
                    if (result == null || result.isBlank()) {
                        toolResults.add(responseFormatter.missingToolParameterError("result"));
                        continue;
                    }
                    stats = stats.withToolUse();
                    return AgentRunResult.completed(
                            result.trim(), UUID.randomUUID().toString(), agentDefinition.agentType(), stats);
                }

                String toolResult = executeTool(toolUse);
                stats = stats.withToolUse();
                toolResults.add(formatToolResult(toolUse, toolResult));
            }
            conversation.add(userMessage(String.join("\n\n", toolResults)));
        }

        return AgentRunResult.failed(
                "Agent exceeded maximum turns without calling attempt_completion.", stats);
    }

    private String readAssistantMessage(List<MessageParam> conversation, AgentRunStats stats) {
        StringBuilder assistantMessage = new StringBuilder();
        Flux<ApiChunk> chunks =
                parentContext
                        .getApiHandler()
                        .createMessageStream(agentDefinition.systemPrompt(), conversation);
        chunks.toIterable()
                .forEach(
                        chunk -> {
                            if (chunk.type() == ApiChunk.ChunkType.TEXT && chunk.text() != null) {
                                assistantMessage.append(chunk.text());
                            } else if (chunk.type() == ApiChunk.ChunkType.USAGE) {
                                stats.addUsage(
                                        chunk.inputTokens(),
                                        chunk.outputTokens(),
                                        chunk.cacheWriteTokens(),
                                        chunk.cacheReadTokens(),
                                        chunk.totalCost());
                            }
                        });
        return assistantMessage.toString();
    }

    private String executeTool(ToolUse toolUse) {
        String toolName = toolUse.getName();
        if (!agentDefinition.allowsTool(toolName)) {
            return responseFormatter.toolError(
                    "Tool '%s' is not available inside this agent.".formatted(toolName));
        }
        ToolHandler<?> handler = parentContext.getCoordinator().getRegistry().getToolHandler(toolName);
        if (handler == null) {
            return responseFormatter.toolError("No handler registered for tool '%s'.".formatted(toolName));
        }
        ToolContext childContext = createChildContext();
        ToolExecuteResult result = ToolHandlerInvocationSupport.invoke(handler, childContext, toolUse);
        if (result instanceof ToolExecuteResult.Immediate immediate) {
            return serializeToolResult(immediate.blocks());
        }
        return responseFormatter.toolError("Tool '%s' returned a non-immediate result.".formatted(toolName));
    }

    private ToolContext createChildContext() {
        return ToolContext.builder()
                .taskId(parentContext.getTaskId())
                .ulid(parentContext.getUlid())
                .cwd(parentContext.getCwd())
                .mode(parentContext.getMode())
                .workspaceManager(parentContext.getWorkspaceManager())
                .yoloModeToggled(parentContext.isYoloModeToggled())
                .doubleCheckCompletionEnabled(parentContext.isDoubleCheckCompletionEnabled())
                .enableParallelToolCalling(parentContext.isEnableParallelToolCalling())
                .isSubagentExecution(true)
                .messageState(parentContext.getMessageState())
                .api(parentContext.getApi())
                .apiHandler(parentContext.getApiHandler())
                .taskState(new TaskState())
                .services(parentContext.getServices())
                .autoApprovalSettings(parentContext.getAutoApprovalSettings())
                .callbacks(parentContext.getCallbacks())
                .coordinator(parentContext.getCoordinator())
                .autoApprover(parentContext.getAutoApprover())
                .systemPromptContext(parentContext.getSystemPromptContext())
                .build();
    }

    private static UserMessage userMessage(String text) {
        return UserMessage.builder().content(List.of(new TextContentBlock(text))).build();
    }

    private static AssistantMessage assistantMessage(String text) {
        return AssistantMessage.builder().content(List.of(new TextContentBlock(text))).build();
    }

    private static String stringParam(Map<String, Object> params, String name) {
        if (params == null) {
            return null;
        }
        Object value = params.get(name);
        return value != null ? String.valueOf(value) : null;
    }

    private static String formatToolResult(ToolUse toolUse, String result) {
        return "%s Result:\n%s".formatted(toolUse.getName(), result);
    }

    private static String serializeToolResult(List<UserContentBlock> blocks) {
        if (blocks == null || blocks.isEmpty()) {
            return "(tool did not return anything)";
        }
        return blocks.stream()
                .map(AgentRunner::serializeBlock)
                .filter(text -> text != null && !text.isBlank())
                .collect(Collectors.joining("\n"));
    }

    private static String serializeBlock(UserContentBlock block) {
        if (block instanceof TextContentBlock textBlock) {
            return textBlock.getText();
        }
        return String.valueOf(block);
    }

    public record AgentRunResult(
            String status, String result, String error, String agentId, String agentType, AgentRunStats stats) {
        public static AgentRunResult completed(
                String result, String agentId, String agentType, AgentRunStats stats) {
            return new AgentRunResult("completed", result, null, agentId, agentType, stats);
        }

        public static AgentRunResult failed(String error, AgentRunStats stats) {
            return new AgentRunResult("failed", null, error, null, null, stats);
        }
    }

    public static final class AgentRunStats {
        private int inputTokens;
        private int outputTokens;
        private int cacheWriteTokens;
        private int cacheReadTokens;
        private double totalCost;
        private int toolUses;

        public static AgentRunStats empty() {
            return new AgentRunStats();
        }

        public AgentRunStats withToolUse() {
            toolUses++;
            return this;
        }

        public void addUsage(
                Integer inputTokens,
                Integer outputTokens,
                Integer cacheWriteTokens,
                Integer cacheReadTokens,
                Double totalCost) {
            this.inputTokens += inputTokens != null ? inputTokens : 0;
            this.outputTokens += outputTokens != null ? outputTokens : 0;
            this.cacheWriteTokens += cacheWriteTokens != null ? cacheWriteTokens : 0;
            this.cacheReadTokens += cacheReadTokens != null ? cacheReadTokens : 0;
            this.totalCost += totalCost != null ? totalCost : 0.0;
        }

        public int inputTokens() {
            return inputTokens;
        }

        public int outputTokens() {
            return outputTokens;
        }

        public int cacheWriteTokens() {
            return cacheWriteTokens;
        }

        public int cacheReadTokens() {
            return cacheReadTokens;
        }

        public double totalCost() {
            return totalCost;
        }

        public int toolUses() {
            return toolUses;
        }

        public int totalTokens() {
            return inputTokens + outputTokens + cacheWriteTokens + cacheReadTokens;
        }
    }
}
