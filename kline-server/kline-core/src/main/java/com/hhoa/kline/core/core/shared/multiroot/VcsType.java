package com.hhoa.kline.core.core.shared.multiroot;

public enum VcsType {
    NONE("none"),

    GIT("git");

    private final String value;

    VcsType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }

    public static VcsType fromValue(String value) {
        if (value == null) {
            return NONE;
        }

        for (VcsType type : VcsType.values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }

        return NONE;
    }
}
