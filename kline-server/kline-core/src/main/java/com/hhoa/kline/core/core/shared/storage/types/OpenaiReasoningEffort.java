package com.hhoa.kline.core.core.shared.storage.types;

public enum OpenaiReasoningEffort {
    MINIMAL("minimal"),
    LOW("low"),
    MEDIUM("medium"),
    HIGH("high");

    private final String value;

    OpenaiReasoningEffort(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }

    public static OpenaiReasoningEffort fromValue(String value) {
        if (value == null) {
            return MINIMAL;
        }

        for (OpenaiReasoningEffort effort : OpenaiReasoningEffort.values()) {
            if (effort.value.equalsIgnoreCase(value)) {
                return effort;
            }
        }

        return MINIMAL;
    }
}
