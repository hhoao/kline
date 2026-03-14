package com.hhoa.kline.core.core.shared;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum McpDisplayMode {
    RICH("rich"),
    PLAIN("plain"),
    MARKDOWN("markdown");

    private String value;

    McpDisplayMode(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static McpDisplayMode fromValue(String value) {
        if (value == null) {
            return PLAIN;
        }

        for (McpDisplayMode mode : McpDisplayMode.values()) {
            if (mode.value.equalsIgnoreCase(value)) {
                return mode;
            }
        }

        return PLAIN;
    }
}
