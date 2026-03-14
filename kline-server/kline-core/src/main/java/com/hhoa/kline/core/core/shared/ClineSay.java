package com.hhoa.kline.core.core.shared;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Getter;

@Getter
public enum ClineSay {
    TASK("task"),
    ERROR("error"),
    ERROR_RETRY("error_retry"),
    API_REQ_STARTED("api_req_started"),
    API_REQ_FINISHED("api_req_finished"),
    TEXT("text"),
    REASONING("reasoning"),
    COMPLETION_RESULT("completion_result"),
    USER_FEEDBACK("user_feedback"),
    USER_FEEDBACK_DIFF("user_feedback_diff"),
    API_REQ_RETRIED("api_req_retried"),
    COMMAND("command"),
    COMMAND_OUTPUT("command_output"),
    TOOL("tool"),
    SHELL_INTEGRATION_WARNING("shell_integration_warning"),
    SHELL_INTEGRATION_WARNING_WITH_SUGGESTION("shell_integration_warning_with_suggestion"),
    BROWSER_ACTION_LAUNCH("browser_action_launch"),
    BROWSER_ACTION("browser_action"),
    BROWSER_ACTION_RESULT("browser_action_result"),
    MCP_SERVER_REQUEST_STARTED("mcp_server_request_started"),
    MCP_SERVER_RESPONSE("mcp_server_response"),
    MCP_NOTIFICATION("mcp_notification"),
    USE_MCP_SERVER("use_mcp_server"),
    DIFF_ERROR("diff_error"),
    DELETED_API_REQS("deleted_api_reqs"),
    CLINEIGNORE_ERROR("clineignore_error"),
    CHECKPOINT_CREATED("checkpoint_created"),
    LOAD_MCP_DOCUMENTATION("load_mcp_documentation"),
    INFO("info"), // Added for general informational messages like retry status
    TASK_PROGRESS("task_progress");

    private final String value;

    ClineSay(String value) {
        this.value = value;
    }

    private static final Map<String, ClineSay> BY_VALUE =
            Arrays.stream(values())
                    .collect(Collectors.toMap(ClineSay::getValue, Function.identity()));

    /**
     * 从字符串值获取枚举（用于 JSON 反序列化）
     *
     * @param value 字符串值
     * @return 对应的枚举值，如果不存在则返回 null
     */
    @JsonCreator
    public static ClineSay fromValue(String value) {
        if (value == null) {
            return null;
        }
        return BY_VALUE.get(value);
    }

    /**
     * 获取字符串值（用于 JSON 序列化）
     *
     * @return 字符串值
     */
    @JsonValue
    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
