package com.hhoa.kline.core.core.shared.proto.host;

public enum SearchWorkspaceItemsRequestSearchItemType {
    FILE(0),
    FOLDER(1),
    UNRECOGNIZED(-1);

    private final int value;

    SearchWorkspaceItemsRequestSearchItemType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static SearchWorkspaceItemsRequestSearchItemType fromJSON(Object object) {
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

    public static SearchWorkspaceItemsRequestSearchItemType fromValue(int value) {
        for (SearchWorkspaceItemsRequestSearchItemType type : values()) {
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
