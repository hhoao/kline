package com.hhoa.kline.core.core.tools.agent;

import java.util.List;

public record AgentDefinition(
        String agentType,
        String whenToUse,
        String systemPrompt,
        List<String> tools,
        List<String> disallowedTools,
        String model) {

    public boolean allowsTool(String toolName) {
        if (toolName == null) {
            return false;
        }
        if (AgentConstants.AGENT_TOOL_NAME.equals(toolName)) {
            return false;
        }
        if (disallowedTools != null && disallowedTools.contains(toolName)) {
            return false;
        }
        return tools == null || tools.isEmpty() || tools.contains("*") || tools.contains(toolName);
    }
}
