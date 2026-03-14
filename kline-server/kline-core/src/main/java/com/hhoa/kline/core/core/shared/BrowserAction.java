package com.hhoa.kline.core.core.shared;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Getter;

@Getter
public enum BrowserAction {
    LAUNCH("launch"),
    CLICK("click"),
    TYPE("type"),
    SCROLL_DOWN("scroll_down"),
    SCROLL_UP("scroll_up"),
    CLOSE("close");

    private final String value;

    BrowserAction(String value) {
        this.value = value;
    }

    public static final List<BrowserAction> BROWSER_ACTIONS = Arrays.asList(values());

    private static final Map<String, BrowserAction> BY_VALUE =
            Arrays.stream(values())
                    .collect(Collectors.toMap(BrowserAction::getValue, Function.identity()));

    /**
     * 从字符串值获取枚举（用于 JSON 反序列化）
     *
     * @param value 字符串值
     * @return 对应的枚举值，如果不存在则返回 null
     */
    @JsonCreator
    public static BrowserAction fromValue(String value) {
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
