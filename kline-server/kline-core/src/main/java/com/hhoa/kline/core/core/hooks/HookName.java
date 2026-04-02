package com.hhoa.kline.core.core.hooks;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Getter;

/** 支持的 Hook 类型枚举 */
@Getter
public enum HookName {
    PRE_TOOL_USE("PreToolUse"),
    POST_TOOL_USE("PostToolUse"),
    USER_PROMPT_SUBMIT("UserPromptSubmit"),
    TASK_START("TaskStart"),
    TASK_RESUME("TaskResume"),
    TASK_CANCEL("TaskCancel"),
    TASK_COMPLETE("TaskComplete"),
    NOTIFICATION("Notification"),
    PRE_COMPACT("PreCompact");

    private final String value;

    HookName(String value) {
        this.value = value;
    }

    private static final Map<String, HookName> BY_VALUE =
            Arrays.stream(values())
                    .collect(Collectors.toMap(HookName::getValue, Function.identity()));

    @JsonCreator
    public static HookName fromValue(String value) {
        return value != null ? BY_VALUE.get(value) : null;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
