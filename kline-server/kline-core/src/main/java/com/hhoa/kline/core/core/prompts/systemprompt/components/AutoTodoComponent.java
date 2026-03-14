package com.hhoa.kline.core.core.prompts.systemprompt.components;

import com.hhoa.kline.core.core.prompts.systemprompt.PromptVariant;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptComponent;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptContext;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptSection;
import com.hhoa.kline.core.core.prompts.systemprompt.templates.TemplateEngine;
import java.util.Map;
import lombok.RequiredArgsConstructor;

/**
 * 自动待办事项组件
 *
 * @author hhoa
 */
@RequiredArgsConstructor
public class AutoTodoComponent implements SystemPromptComponent {

    private final TemplateEngine templateEngine;

    @Override
    public String apply(PromptVariant variant, SystemPromptContext context) {
        if (context.getFocusChainSettings() == null
                || !context.getFocusChainSettings().isEnabled()) {
            return null;
        }

        String template = getTemplateText();

        if (variant.getComponentOverrides() != null
                && variant.getComponentOverrides().containsKey(SystemPromptSection.TODO)) {
            var override = variant.getComponentOverrides().get(SystemPromptSection.TODO);
            if (override.getTemplate() != null) {
                template = override.getTemplate();
            }
        }

        return templateEngine.resolve(template, context, Map.of());
    }

    @Override
    public SystemPromptSection getSystemPromptSection() {
        return SystemPromptSection.TODO;
    }

    private String getTemplateText() {
        return """
            AUTOMATIC TODO LIST MANAGEMENT

            The system automatically manages todo lists to help track task progress:

            - Every 10th API request, you will be prompted to review and update the current todo list if one exists
            - When switching from PLAN MODE to ACT MODE, you should create a comprehensive todo list for the task
            - Todo list updates should be done silently using the task_progress parameter - do not announce these updates to the user
            - Use standard Markdown checklist format: "- [ ]" for incomplete items and "- [x]" for completed items
            - The system will automatically include todo list context in your prompts when appropriate
            - Focus on creating actionable, meaningful steps rather than granular technical details
            """;
    }
}
