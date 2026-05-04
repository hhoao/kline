package com.hhoa.kline.core.core.tools.agent;

import com.hhoa.kline.core.core.tools.ClineDefaultTool;
import java.util.List;

public final class BuiltInAgents {
    private static final String SHARED_PREFIX =
            "You are an agent for Kline. Given the user's message, use the available tools to complete the task. Complete the task fully; do not leave it half-done.";

    private static final String SHARED_GUIDELINES =
            """
            Your strengths:
            - Searching for code, configurations, and patterns across large codebases
            - Analyzing multiple files to understand system architecture
            - Investigating complex questions that require exploring many files
            - Performing multi-step research tasks

            Guidelines:
            - For file searches: search broadly when you do not know where something lives. Use read_file when you know the specific file path.
            - For analysis: start broad and narrow down. Use multiple search strategies if the first does not yield results.
            - Be thorough: check multiple locations, consider different naming conventions, and look for related files.
            - Never create files unless they are absolutely necessary for achieving your goal.
            - When finished, call attempt_completion with a concise report covering what was done and any key findings.
            """;

    private static final String READ_ONLY_SUFFIX =
            """

            === READ-ONLY MODE ===
            You are strictly prohibited from creating, modifying, deleting, moving, or copying files.
            Use execute_command only for read-only operations such as ls, git status, git log, git diff, find, grep, cat, head, and tail.
            End by calling attempt_completion with your findings.
            """;

    private static final List<AgentDefinition> BUILT_INS =
            List.of(
                    new AgentDefinition(
                            AgentConstants.GENERAL_PURPOSE_AGENT_TYPE,
                            "General-purpose agent for researching complex questions, searching code, and executing multi-step tasks.",
                            SHARED_PREFIX + "\n\n" + SHARED_GUIDELINES,
                            List.of("*"),
                            List.of(),
                            null),
                    new AgentDefinition(
                            "Explore",
                            "Fast agent specialized for exploring codebases and answering codebase questions.",
                            "You are a file search specialist for Kline. You excel at navigating and exploring codebases."
                                    + READ_ONLY_SUFFIX,
                            List.of(
                                    ClineDefaultTool.FILE_READ.getValue(),
                                    ClineDefaultTool.LIST_FILES.getValue(),
                                    ClineDefaultTool.SEARCH.getValue(),
                                    ClineDefaultTool.LIST_CODE_DEF.getValue(),
                                    ClineDefaultTool.BASH.getValue(),
                                    ClineDefaultTool.ATTEMPT.getValue()),
                            List.of(),
                            "haiku"),
                    new AgentDefinition(
                            "Plan",
                            "Software architect agent for designing implementation plans.",
                            "You are a software architect and planning specialist for Kline. Explore the codebase and design implementation plans."
                                    + READ_ONLY_SUFFIX
                                    + """

                                    End your response with:

                                    ### Critical Files for Implementation
                                    List 3-5 files most critical for implementing this plan.
                                    """,
                            List.of(
                                    ClineDefaultTool.FILE_READ.getValue(),
                                    ClineDefaultTool.LIST_FILES.getValue(),
                                    ClineDefaultTool.SEARCH.getValue(),
                                    ClineDefaultTool.LIST_CODE_DEF.getValue(),
                                    ClineDefaultTool.BASH.getValue(),
                                    ClineDefaultTool.ATTEMPT.getValue()),
                            List.of(),
                            "inherit"));

    private BuiltInAgents() {}

    public static List<AgentDefinition> all() {
        return BUILT_INS;
    }

    public static AgentDefinition find(String agentType) {
        String effectiveType =
                agentType == null || agentType.isBlank()
                        ? AgentConstants.GENERAL_PURPOSE_AGENT_TYPE
                        : agentType.trim();
        return BUILT_INS.stream()
                .filter(agent -> agent.agentType().equals(effectiveType))
                .findFirst()
                .orElse(null);
    }
}
