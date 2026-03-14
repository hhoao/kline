package com.hhoa.kline.core.core.prompts.systemprompt;

import lombok.Getter;

/**
 * 系统提示部分枚举
 *
 * @author hhoa
 */
@Getter
public enum SystemPromptSection {
    AGENT_ROLE("AGENT_ROLE_SECTION"),
    TOOL_USE("TOOL_USE_SECTION"),
    TOOLS("TOOLS_SECTION"),
    MCP("MCP_SECTION"),
    EDITING_FILES("EDITING_FILES_SECTION"),
    ACT_VS_PLAN("ACT_VS_PLAN_SECTION"),
    CLI_SUBAGENTS("CLI_SUBAGENTS_SECTION"),
    TODO("TODO_SECTION"),
    CAPABILITIES("CAPABILITIES_SECTION"),
    RULES("RULES_SECTION"),
    SYSTEM_INFO("SYSTEM_INFO_SECTION"),
    OBJECTIVE("OBJECTIVE_SECTION"),
    USER_INSTRUCTIONS("USER_INSTRUCTIONS_SECTION"),
    FEEDBACK("FEEDBACK_SECTION"),
    TASK_PROGRESS("TASK_PROGRESS_SECTION"),
    COMPLETE_TRUNCATED_CONTENT("COMPLETE_TRUNCATED_CONTENT_SECTION"),
    SKILLS("SKILLS_SECTION");

    private final String value;

    SystemPromptSection(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
