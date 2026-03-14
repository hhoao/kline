package com.hhoa.kline.core.core.prompts.systemprompt;

/**
 * 模型家族枚举
 *
 * @author hhoa
 */
public enum ModelFamily {
    GENERIC("generic"),
    NEXT_GEN("next-gen"),
    GLM("glm"),
    GPT_5("gpt-5"),
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
}
