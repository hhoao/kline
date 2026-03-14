package com.hhoa.kline.core.core.shared;

public enum TelemetrySetting {
    UNSET("unset"),
    ENABLED("enabled"),
    DISABLED("disabled");

    private final String value;

    TelemetrySetting(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }

    public static TelemetrySetting fromValue(String value) {
        if (value == null) {
            return UNSET;
        }

        for (TelemetrySetting setting : TelemetrySetting.values()) {
            if (setting.value.equalsIgnoreCase(value)) {
                return setting;
            }
        }

        return UNSET;
    }
}
