package com.hhoa.kline.core.enums;

/**
 * Cline 默认工具枚举 与前端 TypeScript ClineDefaultTool 枚举保持一致
 *
 * @author hhoa
 */
public enum ClineDefaultTool {
    ASK("ask_followup_question"),
    ATTEMPT("attempt_completion"),
    BASH("execute_command"),
    FILE_EDIT("replace_in_file"),
    FILE_READ("read_file"),
    FILE_NEW("write_to_file"),
    SEARCH("search_files"),
    LIST_FILES("list_files"),
    LIST_CODE_DEF("list_code_definition_names"),
    BROWSER("browser_action"),
    MCP_USE("use_mcp_tool"),
    MCP_ACCESS("access_mcp_resource"),
    MCP_DOCS("load_mcp_documentation"),
    NEW_TASK("new_task"),
    PLAN_MODE("plan_mode_respond"),
    TODO("focus_chain"),
    WEB_FETCH("web_fetch"),
    CONDENSE("condense"),
    SUMMARIZE_TASK("summarize_task"),
    REPORT_BUG("report_bug"),
    NEW_RULE("new_rule");

    private final String value;

    ClineDefaultTool(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
