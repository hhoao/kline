package com.hhoa.kline.core.core.prompts.commands.deepplanning;

import static com.hhoa.kline.core.core.prompts.systemprompt.ModelFamilyMatchers.isAnthropicModelId;

/**
 * Anthropic Claude variant for deep-planning prompt.
 * Optimized for Claude models.
 * 对应 TS deep-planning/variants/anthropic.ts
 *
 * @author hhoa
 */
public final class AnthropicDeepPlanningVariant {

    private AnthropicDeepPlanningVariant() {}

    private static final String RESEARCH_INSTRUCTIONS =
            """
            ### Required Research Activities
            You must use the read_file tool to examine relevant source files, configuration files, and documentation. \
            You must use terminal commands to gather information about the codebase structure and patterns. \
            All terminal output must be piped to cat for visibility.""";

    public static DeepPlanningVariant create() {
        return DeepPlanningVariant.builder()
                .id("anthropic")
                .description("Deep-planning variant optimized for Anthropic Claude models")
                .family("anthropic")
                .version(1)
                .matcher(context -> {
                    if (context == null
                            || context.getProviderInfo() == null
                            || context.getProviderInfo().getModel() == null) {
                        return false;
                    }
                    String modelId = context.getProviderInfo().getModel().getId();
                    return modelId != null && isAnthropicModelId(modelId);
                })
                .template(DeepPlanningTemplates.generateFourStepTemplate(RESEARCH_INSTRUCTIONS))
                .build();
    }
}
