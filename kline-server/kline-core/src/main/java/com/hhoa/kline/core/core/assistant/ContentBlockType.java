package com.hhoa.kline.core.core.assistant;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Getter;

/**
 * @see <a
 *     href="https://github.com/anthropics/anthropic-sdk-typescript/blob/main/src/resources/messages/messages.ts">TypeScript
 *     ContentBlockParam</a>
 */
@Getter
public enum ContentBlockType {
    TEXT("text"),
    IMAGE("image"),
    TOOL_USE("tool_use"),
    THINKING("thinking"),
    REDACTED_THINKING("redacted_thinking");

    private final String value;

    ContentBlockType(String value) {
        this.value = value;
    }

    private static final Map<String, ContentBlockType> BY_VALUE =
            Arrays.stream(values())
                    .collect(Collectors.toMap(ContentBlockType::getValue, Function.identity()));

    /**
     * 从字符串值获取枚举（用于 JSON 反序列化）
     *
     * @param value 字符串值
     * @return 对应的枚举值，如果不存在则返回 null
     */
    @JsonCreator
    public static ContentBlockType fromValue(String value) {
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
