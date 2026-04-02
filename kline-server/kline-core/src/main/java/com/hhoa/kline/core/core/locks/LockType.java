package com.hhoa.kline.core.core.locks;

import com.fasterxml.jackson.annotation.JsonValue;

/** 锁类型枚举 */
public enum LockType {
    FILE("file"),
    INSTANCE("instance"),
    FOLDER("folder");

    private final String value;

    LockType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
