package com.hhoa.kline.core.core.shared.proto.host;

public enum ShowMessageType {
    ERROR(0),
    INFORMATION(1),
    WARNING(2),
    UNRECOGNIZED(-1);

    private final int value;

    ShowMessageType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static ShowMessageType fromJSON(Object object) {
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

    public static ShowMessageType fromValue(int value) {
        for (ShowMessageType type : values()) {
            if (type.value == value) {
                return type;
            }
        }
        return UNRECOGNIZED;
    }

    public String toJSON() {
        return name();
    }
}
