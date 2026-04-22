package com.hhoa.kline.core.core.prompts.commands.deepplanning;

/**
 * Generic fallback variant for deep-planning prompt. Used when no specific model family matcher
 * applies. 对应 TS deep-planning/variants/generic.ts
 *
 * @author hhoa
 */
public final class GenericDeepPlanningVariant {

    private GenericDeepPlanningVariant() {}

    private static final String RESEARCH_INSTRUCTIONS =
            """
            ### Required Research Activities
            You must use the read_file tool to examine relevant source files, configuration files, and documentation. \
            You must use terminal commands to gather information about the codebase structure and patterns. \
            All terminal output must be piped to cat for visibility.""";

    public static DeepPlanningVariant create() {
        return DeepPlanningVariant.builder()
                .id("generic")
                .description(
                        "Generic fallback variant for deep-planning prompt, used for all models")
                .family("generic")
                .version(1)
                .matcher(context -> true)
                .template(DeepPlanningTemplates.generateFourStepTemplate(RESEARCH_INSTRUCTIONS))
                .build();
    }
}
