package com.hhoa.kline.core.core.prompts.systemprompt;

import com.hhoa.kline.core.core.prompts.systemprompt.registry.ComponentRegistry;
import com.hhoa.kline.core.core.prompts.systemprompt.registry.DefaultComponents;
import com.hhoa.kline.core.core.prompts.systemprompt.registry.PromptBuilder;
import com.hhoa.kline.core.core.prompts.systemprompt.registry.PromptRegistry;
import com.hhoa.kline.core.core.prompts.systemprompt.templates.TemplateEngine;
import com.hhoa.kline.core.core.prompts.systemprompt.variants.VariantRegistry;

public class DefaultSystemPromptServiceFactory {
    public static SystemPromptService createSystemPromptService() {
        ComponentRegistry componentRegistry = new ComponentRegistry();
        TemplateEngine templateEngine = new TemplateEngine();
        DefaultComponents.registerAll(componentRegistry, templateEngine);
        PromptBuilder promptBuilder = new PromptBuilder(componentRegistry, templateEngine);
        VariantRegistry variantRegistry = new VariantRegistry();
        PromptRegistry promptRegistry = new PromptRegistry(promptBuilder, variantRegistry);
        return new SystemPromptService(promptRegistry);
    }
}
