package com.hhoa.kline.core.core.shared;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Getter;

@Getter
public enum ClineAsk {
    FOLLOWUP("followup"),
    PLAN_MODE_RESPOND("plan_mode_respond"),
    COMMAND("command"),
    COMMAND_OUTPUT("command_output"),
    COMPLETION_RESULT("completion_result"),
    TOOL("tool"),
    API_REQ_FAILED("api_req_failed"),
    PROCESS_ASSISTANT_RESPONSE_FAILED("process_assistant_response_failed"),
    RESUME_TASK("resume_task"),
    RESUME_COMPLETED_TASK("resume_completed_task"),
    MISTAKE_LIMIT_REACHED("mistake_limit_reached"),
    AUTO_APPROVAL_MAX_REQ_REACHED("auto_approval_max_req_reached"),
    BROWSER_ACTION_LAUNCH("browser_action_launch"),
    USE_MCP_SERVER("use_mcp_server"),
    NEW_TASK("new_task"),
    CONDENSE("condense"),
    SUMMARIZE_TASK("summarize_task"),
    REPORT_BUG("report_bug"),
    ACT_MODE_RESPOND("act_mode_respond");

    private final String value;

    ClineAsk(String value) {
        this.value = value;
    }

    private static final Map<String, ClineAsk> BY_VALUE =
            Arrays.stream(values())
                    .collect(Collectors.toMap(ClineAsk::getValue, Function.identity()));

    /**
     * 从字符串值获取枚举（用于 JSON 反序列化）
     *
     * @param value 字符串值
     * @return 对应的枚举值，如果不存在则返回 null
     */
    @JsonCreator
    public static ClineAsk fromValue(String value) {
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
}
