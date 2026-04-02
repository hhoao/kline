package com.hhoa.kline.core.core.prompts.systemprompt;

/**
 * 模型家族枚举
 *
 * @author hhoa
 */
public enum ModelFamily {
    CLAUDE("claude"),
    GPT("gpt"),
    GPT_5("gpt-5"),
    NATIVE_GPT_5("native-gpt-5"),
    NATIVE_GPT_5_1("native-gpt-5-1"),
    GEMINI("gemini"),
    GEMINI_3("gemini-3"),
    QWEN("qwen"),
    GLM("glm"),
    HERMES("hermes"),
    DEVSTRAL("devstral"),
    NEXT_GEN("next-gen"),
    TRINITY("trinity"),
    GENERIC("generic"),
    XS("xs"),
    NATIVE_NEXT_GEN("native-next-gen");

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

    /**
     * 判断模型 ID 是否属于 GPT-5 家族
     */
    public static boolean isGPT5ModelFamily(String modelId) {
        if (modelId == null) {
            return false;
        }
        String id = modelId.toLowerCase();
        return id.contains("gpt-5") || id.contains("gpt5");
    }

    /**
     * 判断模型 ID 是否属于 GPT OSS 家族
     */
    public static boolean isGptOssModelFamily(String modelId) {
        if (modelId == null) {
            return false;
        }
        String id = modelId.toLowerCase();
        return id.contains("gpt-oss") || id.contains("gpt_oss");
    }
}
