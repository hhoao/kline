package com.hhoa.kline.core.core.context.management;

public enum EditType {
    UNDEFINED(0),
    NO_FILE_READ(1),
    READ_FILE_TOOL(2),
    ALTER_FILE_TOOL(3),
    FILE_MENTION(4);

    private final int value;

    EditType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
