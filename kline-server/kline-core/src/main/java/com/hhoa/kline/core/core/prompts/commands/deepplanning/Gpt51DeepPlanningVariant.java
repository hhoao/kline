package com.hhoa.kline.core.core.prompts.commands.deepplanning;

import static com.hhoa.kline.core.core.prompts.systemprompt.ModelFamilyMatchers.isGPT51Model;

/**
 * OpenAI GPT-5.1 variant for deep-planning prompt.
 * Uses 5-step process with separate read and terminal investigation phases.
 * Template is dynamically generated based on focus chain and native tool call settings.
 * 对应 TS deep-planning/variants/gpt51.ts
 *
 * @author hhoa
 */
public final class Gpt51DeepPlanningVariant {

    private Gpt51DeepPlanningVariant() {}

    public static DeepPlanningVariant create() {
        return DeepPlanningVariant.builder()
                .id("gpt-5")
                .description("Deep-planning variant optimized for OpenAI GPT-5 models")
                .family("gpt-5")
                .version(1)
                .matcher(context -> {
                    if (context == null
                            || context.getProviderInfo() == null
                            || context.getProviderInfo().getModel() == null) {
                        return false;
                    }
                    String modelId = context.getProviderInfo().getModel().getId();
                    return modelId != null && isGPT51Model(modelId);
                })
                .template("") // Template is dynamically generated
                .build();
    }

    /**
     * Generates the deep-planning template dynamically.
     * Structure is identical to Gemini3 variant.
     *
     * @param focusChainEnabled Whether focus chain (task_progress) is enabled
     * @param enableNativeToolCalls Whether native tool calling is enabled
     */
    public static String generateTemplate(boolean focusChainEnabled, boolean enableNativeToolCalls) {
        // GPT-5.1 uses the same 5-step structure as Gemini 3
        return Gemini3DeepPlanningVariant.generateTemplate(focusChainEnabled, enableNativeToolCalls);
    }
}
