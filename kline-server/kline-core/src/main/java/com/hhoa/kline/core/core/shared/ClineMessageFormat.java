package com.hhoa.kline.core.core.shared;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Getter;

@Getter
public enum ClineMessageFormat {
    JSON("json"),
    PLAIN("plain");

    private final String value;

    ClineMessageFormat(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    private static final Map<String, ClineMessageFormat> BY_VALUE =
            Arrays.stream(values())
                    .collect(Collectors.toMap(ClineMessageFormat::getValue, Function.identity()));

    @JsonCreator
    public static ClineMessageFormat fromValue(String value) {
        if (value == null) {
            return null;
        }
        ClineMessageFormat f = BY_VALUE.get(value.toLowerCase());
        return f != null ? f : BY_VALUE.get(value);
    }
}
