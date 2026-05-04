package com.hhoa.kline.core.core.prompts.systemprompt.components;

import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import com.hhoa.kline.core.core.prompts.systemprompt.PromptVariant;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptComponent;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptContext;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptSection;
import com.hhoa.kline.core.core.prompts.systemprompt.templates.TemplateEngine;
import java.util.Map;
import lombok.RequiredArgsConstructor;

/**
 * 任务进度组件
 *
 * @author hhoa
 */
@RequiredArgsConstructor
public class TaskProgressComponent implements SystemPromptComponent {

    private final TemplateEngine templateEngine;

    @Override
    public String apply(PromptVariant variant, SystemPromptContext context) {
        if (context.getFocusChainSettings() == null
                || !context.getFocusChainSettings().isEnabled()) {
            return null;
        }

        // Check for component override first
        if (variant.getComponentOverrides() != null
                && variant.getComponentOverrides().containsKey(SystemPromptSection.TASK_PROGRESS)) {
            var override = variant.getComponentOverrides().get(SystemPromptSection.TASK_PROGRESS);
            if (override.getTemplate() != null) {
                return templateEngine.resolve(override.getTemplate(), context, Map.of());
            }
            if (override.getTemplateFunction() != null) {
                String result = override.getTemplateFunction().apply(context);
                return templateEngine.resolve(result, context, Map.of());
            }
        }

        // Select template based on model family
        String template = getTemplateText();
        if (variant.getFamily() == ModelFamily.NATIVE_NEXT_GEN) {
            template = getNativeNextGenTemplateText();
        } else if (variant.getFamily() == ModelFamily.NATIVE_GPT_5) {
            template = getNativeGpt5TemplateText();
        }

        return templateEngine.resolve(template, context, Map.of());
    }

    @Override
    public SystemPromptSection getSystemPromptSection() {
        return SystemPromptSection.TASK_PROGRESS;
    }

    private String getNativeNextGenTemplateText() {
        return getTodoWriteTemplateText();
    }

    private String getNativeGpt5TemplateText() {
        return getTodoWriteTemplateText();
    }

    private String getTemplateText() {
        return getTodoWriteTemplateText();
    }

    private String getTodoWriteTemplateText() {
        return """
            UPDATING TASK PROGRESS

            Use the TodoWrite tool to track and communicate progress on the overall task. TodoWrite is a standalone tool call, not a parameter on other tools.

            - When switching from PLAN MODE to ACT MODE, create a comprehensive todo list with TodoWrite
            - Todo list updates should be done silently with TodoWrite - do not announce these updates to the user
            - TodoWrite items are structured objects with content, status, and activeForm
            - Status must be one of: pending, in_progress, completed
            - Keep exactly one item in_progress while work is underway whenever possible
            - Keep items focused on meaningful progress milestones rather than minor technical details. The checklist should not be so granular that minor implementation details clutter the progress tracking.
            - For simple tasks, short checklists with even a single item are acceptable. For complex tasks, avoid making the checklist too long or verbose.
            - Provide the whole checklist of steps you intend to complete, and keep the statuses updated as you make progress. It is okay to rewrite this checklist as needed if it becomes invalid due to scope changes or new information.
            - If a checklist is being used, update it any time a step has been completed.
            - The system will automatically include todo list context in your prompts when appropriate - these reminders are important.
            """;
    }
}
