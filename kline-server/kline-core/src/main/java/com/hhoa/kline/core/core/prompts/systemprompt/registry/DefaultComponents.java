package com.hhoa.kline.core.core.prompts.systemprompt.registry;

import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptComponent;
import com.hhoa.kline.core.core.prompts.systemprompt.components.ActVsPlanModeComponent;
import com.hhoa.kline.core.core.prompts.systemprompt.components.AgentRoleComponent;
import com.hhoa.kline.core.core.prompts.systemprompt.components.AutoTodoComponent;
import com.hhoa.kline.core.core.prompts.systemprompt.components.CapabilitiesComponent;
import com.hhoa.kline.core.core.prompts.systemprompt.components.CompleteTruncatedContentComponent;
import com.hhoa.kline.core.core.prompts.systemprompt.components.EditingFilesComponent;
import com.hhoa.kline.core.core.prompts.systemprompt.components.FeedbackComponent;
import com.hhoa.kline.core.core.prompts.systemprompt.components.McpComponent;
import com.hhoa.kline.core.core.prompts.systemprompt.components.ObjectiveComponent;
import com.hhoa.kline.core.core.prompts.systemprompt.components.RulesComponent;
import com.hhoa.kline.core.core.prompts.systemprompt.components.SkillsComponent;
import com.hhoa.kline.core.core.prompts.systemprompt.components.SystemInfoComponent;
import com.hhoa.kline.core.core.prompts.systemprompt.components.TaskProgressComponent;
import com.hhoa.kline.core.core.prompts.systemprompt.components.ToolUseComponent;
import com.hhoa.kline.core.core.prompts.systemprompt.components.UserInstructionsComponent;
import com.hhoa.kline.core.core.prompts.systemprompt.templates.TemplateEngine;

public final class DefaultComponents {

    private DefaultComponents() {}

    public static void registerAll(
            ComponentRegistry registry, TemplateEngine templateEngine, PromptBuilder promptBuilder) {
        register(registry, new ActVsPlanModeComponent(templateEngine));
        register(registry, new AgentRoleComponent(templateEngine));
        register(registry, new AutoTodoComponent(templateEngine));
        register(registry, new CapabilitiesComponent(templateEngine));
        register(registry, new CompleteTruncatedContentComponent(templateEngine));
        register(registry, new EditingFilesComponent(templateEngine));
        register(registry, new FeedbackComponent(templateEngine));
        register(registry, new McpComponent(templateEngine));
        register(registry, new ObjectiveComponent(templateEngine));
        register(registry, new RulesComponent(templateEngine));
        register(registry, new SkillsComponent(templateEngine));
        register(registry, new SystemInfoComponent(templateEngine));
        register(registry, new TaskProgressComponent(templateEngine));
        register(registry, new ToolUseComponent(templateEngine, promptBuilder));
        register(registry, new UserInstructionsComponent(templateEngine));
    }

    private static void register(ComponentRegistry registry, SystemPromptComponent component) {
        registry.register(component.getSystemPromptSection(), component);
    }
}
