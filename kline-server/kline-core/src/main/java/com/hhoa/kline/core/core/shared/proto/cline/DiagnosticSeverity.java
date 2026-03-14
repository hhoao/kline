package com.hhoa.kline.core.core.shared.proto.cline;

public enum DiagnosticSeverity {
    DIAGNOSTIC_ERROR(0),
    DIAGNOSTIC_WARNING(1),
    DIAGNOSTIC_INFORMATION(2),
    DIAGNOSTIC_HINT(3),
    UNRECOGNIZED(-1);

    private final int value;

    DiagnosticSeverity(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static DiagnosticSeverity fromJSON(Object object) {
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

    public static DiagnosticSeverity fromValue(int value) {
        for (DiagnosticSeverity severity : values()) {
            if (severity.value == value) {
                return severity;
            }
        }
        return UNRECOGNIZED;
    }

    public String toJSON() {
        return name();
    }
}
