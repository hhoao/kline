package com.hhoa.kline.core.core.shared;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Getter;

@Getter
public enum ClineApiReqCancelReason {
    STREAMING_FAILED("streaming_failed"),
    USER_CANCELLED("user_cancelled"),
    USER_FEEDBACK("user_feedback"),
    RETRIES_EXHAUSTED("retries_exhausted");

    private final String value;

    ClineApiReqCancelReason(String value) {
        this.value = value;
    }

    private static final Map<String, ClineApiReqCancelReason> BY_VALUE =
            Arrays.stream(values())
                    .collect(
                            Collectors.toMap(
                                    ClineApiReqCancelReason::getValue, Function.identity()));

    /**
     * 从字符串值获取枚举（用于 JSON 反序列化）
     *
     * @param value 字符串值
     * @return 对应的枚举值，如果不存在则返回 null
     */
    @JsonCreator
    public static ClineApiReqCancelReason fromValue(String value) {
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
