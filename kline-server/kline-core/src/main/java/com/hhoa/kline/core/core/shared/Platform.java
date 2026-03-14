package com.hhoa.kline.core.core.shared;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Getter;

@Getter
public enum Platform {
    AIX("aix"),
    DARWIN("darwin"),
    FREEBSD("freebsd"),
    LINUX("linux"),
    OPENBSD("openbsd"),
    SUNOS("sunos"),
    WIN32("win32"),
    UNKNOWN("unknown");

    private final String value;

    Platform(String value) {
        this.value = value;
    }

    public static final Platform DEFAULT_PLATFORM = UNKNOWN;

    private static final Map<String, Platform> BY_VALUE =
            Arrays.stream(values())
                    .collect(Collectors.toMap(Platform::getValue, Function.identity()));

    /**
     * 从字符串值获取枚举（用于 JSON 反序列化）
     *
     * @param value 字符串值
     * @return 对应的枚举值，如果不存在则返回 DEFAULT_PLATFORM（默认值）
     */
    @JsonCreator
    public static Platform fromValue(String value) {
        if (value == null) {
            return DEFAULT_PLATFORM;
        }
        return BY_VALUE.getOrDefault(value, DEFAULT_PLATFORM);
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
