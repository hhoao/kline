package com.hhoa.kline.core.core.shared;

public enum TaskFeedbackType {
    THUMBS_UP("thumbs_up"),
    THUMBS_DOWN("thumbs_down");

    private final String value;

    TaskFeedbackType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }

    public static TaskFeedbackType fromValue(String value) {
        if (value == null) {
            return THUMBS_UP;
        }
        for (TaskFeedbackType type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        return THUMBS_UP;
    }
}
