package com.hhoa.kline.core.core.shared;

public enum ModelFamily {
    CLAUDE("claude"),
    GPT("gpt"),
    GPT_5("gpt-5"),
    GEMINI("gemini"),
    QWEN("qwen"),
    GLM("glm"),
    NEXT_GEN("next-gen"),
    GENERIC("generic"),
    XS("xs");

    private final String value;

    ModelFamily(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }

    public static ModelFamily fromValue(String value) {
        if (value == null) {
            return GENERIC;
        }
        for (ModelFamily family : values()) {
            if (family.value.equals(value)) {
                return family;
            }
        }
        return GENERIC;
    }
}
