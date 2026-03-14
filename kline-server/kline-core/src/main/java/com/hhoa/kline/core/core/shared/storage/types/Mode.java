package com.hhoa.kline.core.core.shared.storage.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Getter;

@Getter
public enum Mode {
    PLAN("plan"),
    ACT("act");

    private final String value;

    Mode(String value) {
        this.value = value;
    }

    private static final Map<String, Mode> BY_VALUE =
            Arrays.stream(values())
                    .collect(
                            Collectors.toMap(
                                    mode -> mode.value.toLowerCase(Locale.ROOT),
                                    Function.identity()));

    @JsonCreator
    public static Mode fromValue(String value) {
        if (value == null) {
            return PLAN;
        }
        Mode mode = BY_VALUE.get(value.toLowerCase(Locale.ROOT));
        return mode != null ? mode : PLAN;
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
