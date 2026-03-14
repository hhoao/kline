package com.hhoa.kline.core.core.shared;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Getter;

@Getter
public enum ClineCheckpointRestore {
    TASK("task"),
    WORKSPACE("workspace"),
    TASK_AND_WORKSPACE("taskAndWorkspace");

    private final String value;

    ClineCheckpointRestore(String value) {
        this.value = value;
    }

    private static final Map<String, ClineCheckpointRestore> BY_VALUE =
            Arrays.stream(values())
                    .collect(
                            Collectors.toMap(
                                    ClineCheckpointRestore::getValue, Function.identity()));

    /**
     * 从字符串值获取枚举（用于 JSON 反序列化）
     *
     * @param value 字符串值
     * @return 对应的枚举值，如果不存在则返回 TASK（默认值）
     */
    @JsonCreator
    public static ClineCheckpointRestore fromValue(String value) {
        if (value == null) {
            return TASK;
        }
        ClineCheckpointRestore restore = BY_VALUE.get(value);
        return restore != null ? restore : TASK;
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
