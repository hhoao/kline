package com.hhoa.kline.core.core.tools.handlers;

import com.hhoa.kline.core.core.assistant.ToolUse;
import com.hhoa.kline.core.core.prompts.ResponseFormatter;
import com.hhoa.kline.core.core.shared.ClineSay;
import com.hhoa.kline.core.core.tools.agent.AgentCatalog;
import com.hhoa.kline.core.core.tools.agent.AgentDefinition;
import com.hhoa.kline.core.core.tools.agent.AgentRunner;
import com.hhoa.kline.core.core.tools.args.AgentInput;
import com.hhoa.kline.core.core.tools.types.ToolContext;
import com.hhoa.kline.core.core.tools.types.ToolExecuteResult;
import java.util.List;

public class AgentToolHandler implements ToolHandler<AgentInput> {
    private final ResponseFormatter responseFormatter = new ResponseFormatter();

    @Override
    public String getDescription(ToolUse block) {
        return "[Agent]";
    }

    @Override
    public boolean isConcurrencySafe(ToolUse block, ToolContext context) {
        return true;
    }

    @Override
    public void handlePartialBlock(AgentInput input, ToolContext context, ToolUse block) {
        if (context.getCallbacks() != null) {
            context.getCallbacks()
                    .say(
                            ClineSay.SUBAGENT,
                            input != null ? input.description() : "",
                            null,
                            null,
                            true,
                            null);
        }
    }

    @Override
    public ToolExecuteResult execute(AgentInput input, ToolContext context, ToolUse block) {
        String validationError = validate(input);
        if (validationError != null) {
            return HandlerUtils.createToolExecuteResult(responseFormatter.toolError(validationError));
        }
        if (Boolean.TRUE.equals(input.runInBackground())) {
            return HandlerUtils.createToolExecuteResult(
                    responseFormatter.toolError(
                            "Background Agent execution is not available in this Kline runtime yet."));
        }

        AgentDefinition agentDefinition = AgentCatalog.find(input.subagentType(), context.getCwd());
        if (agentDefinition == null) {
            String available =
                    String.join(
                            ", ",
                            AgentCatalog.load(context.getCwd()).stream()
                                    .map(AgentDefinition::agentType)
                                    .toList());
            return HandlerUtils.createToolExecuteResult(
                    responseFormatter.toolError(
                            "Agent type '%s' not found. Available agents: %s"
                                    .formatted(input.subagentType(), available)));
        }

        if (context.getCallbacks() != null) {
            context.getCallbacks()
                    .say(ClineSay.SUBAGENT, input.description(), null, null, false, null);
        }

        long start = System.currentTimeMillis();
        AgentRunner.AgentRunResult result = new AgentRunner(context, agentDefinition).run(input.prompt());
        if (!"completed".equals(result.status())) {
            return HandlerUtils.createToolExecuteResult(responseFormatter.toolError(result.error()));
        }

        AgentRunner.AgentRunStats stats = result.stats();
        String text =
                """
                %s

                agentId: %s
                agentType: %s
                <usage>total_tokens: %d
                tool_uses: %d
                duration_ms: %d</usage>
                """
                        .formatted(
                                result.result(),
                                result.agentId(),
                                result.agentType(),
                                stats.totalTokens(),
                                stats.toolUses(),
                                System.currentTimeMillis() - start)
                        .trim();
        return new ToolExecuteResult.Immediate(List.of(new com.hhoa.kline.core.core.assistant.TextContentBlock(text)));
    }

    private static String validate(AgentInput input) {
        if (input == null) {
            return "Missing Agent input.";
        }
        if (input.description() == null || input.description().isBlank()) {
            return "Missing required parameter: description.";
        }
        if (input.prompt() == null || input.prompt().isBlank()) {
            return "Missing required parameter: prompt.";
        }
        return null;
    }
}
