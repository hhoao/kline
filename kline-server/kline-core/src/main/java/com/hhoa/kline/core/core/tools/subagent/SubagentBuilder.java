package com.hhoa.kline.core.core.tools.subagent;

import com.hhoa.kline.core.core.tools.types.ToolContext;
import com.hhoa.kline.core.enums.ClineDefaultTool;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** 与 Cline {@code SubagentBuilder.ts} 对齐：为子代理构建配置（允许工具、系统提示、API handler）。 */
public class SubagentBuilder {

    public static final List<ClineDefaultTool> SUBAGENT_DEFAULT_ALLOWED_TOOLS =
            List.of(
                    ClineDefaultTool.FILE_READ,
                    ClineDefaultTool.LIST_FILES,
                    ClineDefaultTool.SEARCH,
                    ClineDefaultTool.LIST_CODE_DEF,
                    ClineDefaultTool.BASH,
                    ClineDefaultTool.USE_SKILL,
                    ClineDefaultTool.ATTEMPT);

    public static final String SUBAGENT_SYSTEM_SUFFIX =
            """


            # Subagent Execution Mode
            You are running as a research subagent. Your job is to explore the codebase and gather information to answer the question.
            Explore, read related files, trace through call chains, and build a complete picture before reporting back.
            You can read files, list directories, search for patterns, list code definitions, and run commands.
            Only use execute_command for readonly operations like ls, grep, git log, git diff, gh, etc.
            When it makes sense, be clever about chaining commands or in-command scripting in execute_command to quickly get relevant context - and using pipes / filters to help narrow results.
            Do not run commands that modify files or system state.
            When you have a comprehensive answer, call the attempt_completion tool.
            The attempt_completion result field is sent directly to the main agent, so put your full final findings there.
            Unless the subagent prompt explicitly asks for detailed analysis, keep the result concise and focus on the files the main agent should read next.
            Include a section titled "Relevant file paths" and list only file paths, one per line.
            Do not include line numbers, summaries, or per-file explanations unless explicitly requested.
            """;

    private final AgentBaseConfig agentConfig;
    private final List<ClineDefaultTool> allowedTools;

    public SubagentBuilder(ToolContext baseConfig, String subagentName) {
        AgentBaseConfig loaded = AgentConfigLoader.getInstance().getCachedConfig(subagentName);
        this.agentConfig = loaded;
        this.allowedTools = resolveAllowedTools(loaded != null ? loaded.tools() : null);
    }

    public List<ClineDefaultTool> getAllowedTools() {
        return allowedTools;
    }

    public List<String> getConfiguredSkills() {
        return agentConfig != null ? agentConfig.skills() : null;
    }

    public String buildSystemPrompt(String generatedSystemPrompt) {
        String configuredSystemPrompt =
                agentConfig != null && agentConfig.systemPrompt() != null
                        ? agentConfig.systemPrompt().trim()
                        : null;
        String systemPrompt =
                configuredSystemPrompt != null && !configuredSystemPrompt.isEmpty()
                        ? configuredSystemPrompt
                        : generatedSystemPrompt;
        return systemPrompt + buildAgentIdentitySystemPrefix() + SUBAGENT_SYSTEM_SUFFIX;
    }

    public String getModelIdOverride() {
        if (agentConfig == null || agentConfig.modelId() == null) {
            return null;
        }
        String trimmed = agentConfig.modelId().trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private List<ClineDefaultTool> resolveAllowedTools(List<ClineDefaultTool> configuredTools) {
        List<ClineDefaultTool> sourceTools =
                configuredTools != null && !configuredTools.isEmpty()
                        ? configuredTools
                        : SUBAGENT_DEFAULT_ALLOWED_TOOLS;
        Set<ClineDefaultTool> set = new LinkedHashSet<>(sourceTools);
        set.add(ClineDefaultTool.ATTEMPT);
        return List.copyOf(set);
    }

    private String buildAgentIdentitySystemPrefix() {
        if (agentConfig == null) {
            return "";
        }
        String name = agentConfig.name() != null ? agentConfig.name().trim() : null;
        String description =
                agentConfig.description() != null ? agentConfig.description().trim() : null;
        if ((name == null || name.isEmpty()) && (description == null || description.isEmpty())) {
            return "";
        }

        List<String> lines = new ArrayList<>();
        lines.add("# Agent Profile");
        if (name != null && !name.isEmpty()) {
            lines.add("Name: " + name);
        }
        if (description != null && !description.isEmpty()) {
            lines.add("Description: " + description);
        }

        return String.join("\n", lines) + "\n\n";
    }
}
