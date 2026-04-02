package com.hhoa.kline.core.core.prompts.commands.deepplanning;

import static com.hhoa.kline.core.core.prompts.systemprompt.ModelFamilyMatchers.isGemini2dot5ModelFamily;

/**
 * Google Gemini 2.5 variant for deep-planning prompt.
 * Enhanced research instructions for Gemini 2.5 models.
 * 对应 TS deep-planning/variants/gemini.ts
 *
 * @author hhoa
 */
public final class GeminiDeepPlanningVariant {

    private GeminiDeepPlanningVariant() {}

    private static final String RESEARCH_INSTRUCTIONS =
            """
            ### Required Research Activities
            You must first use the read_file tool to examine several source files, configuration files, and documentation \
            to better inform subsequent research steps. You should only use read_file to prepare for more granular searching. \
            Use this tool to determine the language(s) used in the codebase, and to identify the domain(s) relevant to the user's request.

            You must then use terminal commands to gather information about the codebase structure and patterns relevant to the user's request. \
            All terminal output must be piped to cat for visibility.
            You will tailor these commands to explore and identify key functions, classes, methods, types, and variables that are directly, or indirectly related to the task.
            These commands must be crafted to not produce exceptionally long or verbose search results. For example, you should exclude dependency folders such as node_modules, venv or php vendor, etc. \
            Carefully consider the scope of search patterns. Use the results of your read_file tool calls to tailor the commands for balanced search result lengths. \
            If a command returns no results, you may loosen the search patterns or scope slightly. \
            If a command returns hundreds or thousands of results, you should adjust subsequent commands to be more targeted.

            Execute these commands to build your understanding. Adjust subsequent commands based on the output you have received from each previous command, \
            informing the scope and direction of your search.

            Here are some example commands, remember to adjust them as instructed previously:""";

    public static DeepPlanningVariant create() {
        return DeepPlanningVariant.builder()
                .id("gemini")
                .description("Deep-planning variant optimized for Google Gemini 2.5 models")
                .family("gemini")
                .version(1)
                .matcher(context -> {
                    if (context == null
                            || context.getProviderInfo() == null
                            || context.getProviderInfo().getModel() == null) {
                        return false;
                    }
                    String modelId = context.getProviderInfo().getModel().getId();
                    return modelId != null && isGemini2dot5ModelFamily(modelId);
                })
                .template(DeepPlanningTemplates.generateFourStepTemplate(RESEARCH_INSTRUCTIONS))
                .build();
    }
}
