package com.hhoa.kline.core.core.assistant;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Getter;

/** 消息角色枚举，用于 JSON 序列化和反序列化 支持大小写不敏感的反序列化 */
@Getter
public enum MessageRole {
    ASSISTANT("assistant"),
    USER("user");

    private final String value;

    MessageRole(String value) {
        this.value = value;
    }

    private static final Map<String, MessageRole> BY_VALUE =
            Arrays.stream(values())
                    .collect(
                            Collectors.toMap(
                                    role -> role.value.toLowerCase(),
                                    Function.identity(),
                                    (existing, replacement) -> existing));

    @JsonCreator
    public static MessageRole fromValue(String value) {
        if (value == null) {
            return null;
        }
        return BY_VALUE.get(value.toLowerCase());
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
