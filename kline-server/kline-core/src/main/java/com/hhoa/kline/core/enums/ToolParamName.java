package com.hhoa.kline.core.enums;

/**
 * 工具参数名称枚举
 *
 * @author hhoa
 */
public enum ToolParamName {
    COMMAND("command"),
    REQUIRES_APPROVAL("requires_approval"),
    PATH("path"),
    CONTENT("content"),
    DIFF("diff"),
    REGEX("regex"),
    FILE_PATTERN("file_pattern"),
    RECURSIVE("recursive"),
    ACTION("action"),
    URL("url"),
    COORDINATE("coordinate"),
    TEXT("text"),
    SERVER_NAME("server_name"),
    TOOL_NAME("tool_name"),
    ARGUMENTS("arguments"),
    URI("uri"),
    QUESTION("question"),
    OPTIONS("options"),
    RESPONSE("response"),
    RESULT("result"),
    CONTEXT("context"),
    TITLE("title"),
    WHAT_HAPPENED("what_happened"),
    STEPS_TO_REPRODUCE("steps_to_reproduce"),
    API_REQUEST_OUTPUT("api_request_output"),
    ADDITIONAL_CONTEXT("additional_context"),
    NEEDS_MORE_EXPLORATION("needs_more_exploration"),
    TASK_PROGRESS("task_progress"),
    START_LINE("start_line"),
    END_LINE("end_line"),
    TIMEOUT("timeout");

    private final String value;

    ToolParamName(String value) {
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
