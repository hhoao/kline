package com.hhoa.kline.core.core.shared.proto.host;

public enum Setting {
    UNSUPPORTED(0),
    ENABLED(1),
    DISABLED(2),
    UNRECOGNIZED(-1);

    private final int value;

    Setting(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static Setting fromJSON(Object object) {
        if (object instanceof Number) {
            int intValue = ((Number) object).intValue();
            return fromValue(intValue);
        } else if (object instanceof String) {
            try {
                return valueOf((String) object);
            } catch (IllegalArgumentException e) {
                return UNRECOGNIZED;
            }
        }
        return UNRECOGNIZED;
    }

    public static Setting fromValue(int value) {
        for (Setting setting : values()) {
            if (setting.value == value) {
                return setting;
            }
        }
        return UNRECOGNIZED;
    }

    public String toJSON() {
        return name();
    }
}
