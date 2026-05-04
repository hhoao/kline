package com.hhoa.kline.core.core.prompts.systemprompt;

import com.hhoa.kline.core.core.prompts.systemprompt.registry.ComponentRegistry;
import com.hhoa.kline.core.core.prompts.systemprompt.registry.DefaultComponents;
import com.hhoa.kline.core.core.prompts.systemprompt.registry.PromptBuilder;
import com.hhoa.kline.core.core.prompts.systemprompt.registry.PromptRegistry;
import com.hhoa.kline.core.core.prompts.systemprompt.templates.TemplateEngine;
import com.hhoa.kline.core.core.prompts.systemprompt.variants.VariantRegistry;
import com.hhoa.kline.core.core.tools.DefaultToolRegistry;
import com.hhoa.kline.core.core.tools.ToolRegistry;

public class DefaultSystemPromptServiceFactory {
    public static SystemPromptService createSystemPromptService() {
        ComponentRegistry componentRegistry = new ComponentRegistry();
        TemplateEngine templateEngine = new TemplateEngine();
        ToolRegistry toolRegistry = new DefaultToolRegistry();
        PromptBuilder promptBuilder =
                new PromptBuilder(componentRegistry, templateEngine, toolRegistry);
        DefaultComponents.registerAll(componentRegistry, templateEngine, promptBuilder);
        VariantRegistry variantRegistry = new VariantRegistry();
        PromptRegistry promptRegistry = new PromptRegistry(promptBuilder, variantRegistry);
        return new SystemPromptService(promptRegistry);
    }
}
