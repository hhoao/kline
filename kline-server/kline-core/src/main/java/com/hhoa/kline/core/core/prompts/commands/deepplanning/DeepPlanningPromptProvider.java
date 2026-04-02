package com.hhoa.kline.core.core.prompts.commands.deepplanning;

import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptContext;

/**
 * Entry point for deep-planning prompt generation.
 * Selects the appropriate variant and generates the prompt with focus chain and native tool call settings.
 * 对应 TS deep-planning/index.ts getDeepPlanningPrompt()
 *
 * @author hhoa
 */
public final class DeepPlanningPromptProvider {

    private DeepPlanningPromptProvider() {}

    /**
     * Generates the deep-planning slash command response with model-family-aware variant selection.
     *
     * @param focusChainEnabled Whether focus chain is enabled
     * @param providerInfo Provider info for model family detection (as SystemPromptContext.ApiProviderInfo)
     * @param enableNativeToolCalls Whether native tool calling is enabled
     * @return The deep-planning prompt string with appropriate variant applied
     */
    public static String getDeepPlanningPrompt(
            Boolean focusChainEnabled,
            SystemPromptContext.ApiProviderInfo providerInfo,
            Boolean enableNativeToolCalls) {

        boolean focusEnabled = Boolean.TRUE.equals(focusChainEnabled);
        boolean nativeTools = Boolean.TRUE.equals(enableNativeToolCalls);

        // Create context for variant selection
        SystemPromptContext context = new SystemPromptContext();
        context.setProviderInfo(
                providerInfo != null ? providerInfo : new SystemPromptContext.ApiProviderInfo());
        context.setIde("vscode");

        // Get the appropriate variant from registry
        DeepPlanningRegistry registry = DeepPlanningRegistry.getInstance();
        DeepPlanningVariant variant = registry.get(context);
        String focusChainParam = focusEnabled ? DeepPlanningTemplates.FOCUS_CHAIN_INTRO : "";
        String newTaskInstructions = DeepPlanningTemplates.generateNewTaskInstructions(nativeTools);

        // For variants with dynamic templates, generate template with parameters
        String template;
        if ("gpt-5".equals(variant.getId())) {
            template = Gpt51DeepPlanningVariant.generateTemplate(focusEnabled, nativeTools);
        } else if ("gemini-3".equals(variant.getId())) {
            template = Gemini3DeepPlanningVariant.generateTemplate(focusEnabled, nativeTools);
        } else {
            template = variant.getTemplate();
            template = template.replace("{{FOCUS_CHAIN_PARAM}}", focusChainParam);
            template = template.replace("{{NEW_TASK_INSTRUCTIONS}}", newTaskInstructions);
        }

        return template;
    }
}
