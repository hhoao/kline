package com.hhoa.kline.core.core.tools.specs;

import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptContext;
import com.hhoa.kline.core.core.tools.ToolSpecProvider;
import com.hhoa.kline.core.core.tools.agent.AgentConstants;
import com.hhoa.kline.core.core.tools.agent.AgentDefinition;
import com.hhoa.kline.core.core.tools.agent.BuiltInAgents;
import com.hhoa.kline.core.core.tools.args.AgentInput;
import com.hhoa.kline.core.core.tools.handlers.AgentToolHandler;
import com.hhoa.kline.core.core.tools.handlers.ToolHandler;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class AgentTool implements ToolSpecProvider<AgentInput> {

    private static final AgentToolHandler HANDLER = new AgentToolHandler();

    @Override
    public String name() {
        return AgentConstants.AGENT_TOOL_NAME;
    }

    @Override
    public String description(ModelFamily family) {
        return "Launch a new agent to handle complex tasks autonomously.";
    }

    @Override
    public String prompt(ModelFamily family) {
        String agentList =
                BuiltInAgents.all().stream()
                        .map(this::formatAgentLine)
                        .collect(Collectors.joining("\n"));
        return """
                Launch a new agent to handle complex, multi-step tasks autonomously.

                Available agent types and when to use them:
                %s

                When using the Agent tool, specify a subagent_type parameter to select which agent type to use. If omitted, the general-purpose agent is used.

                Usage notes:
                - Always include a short description (3-5 words) summarizing what the agent will do.
                - The agent starts fresh. Brief it like a smart colleague who just walked into the room.
                - Custom agents can be defined as markdown files under .claude/agents or ~/.claude/agents and selected with subagent_type.
                - Clearly tell the agent whether you expect it to write code or just do research.
                - The agent's result is returned to you; summarize it for the user when useful.
                """
                .formatted(agentList);
    }

    @Override
    public Function<SystemPromptContext, Boolean> contextRequirements(ModelFamily family) {
        return context -> context == null || !Boolean.TRUE.equals(context.getIsSubagentRun());
    }

    private String formatAgentLine(AgentDefinition agent) {
        String tools =
                agent.tools() == null || agent.tools().isEmpty()
                        ? "All tools"
                        : String.join(", ", agent.tools());
        return "- %s: %s (Tools: %s)".formatted(agent.agentType(), agent.whenToUse(), tools);
    }

    @Override
    public Class<AgentInput> inputType(ModelFamily family) {
        return AgentInput.class;
    }

    @Override
    public ToolHandler<AgentInput> handler(ModelFamily family) {
        return HANDLER;
    }
}
