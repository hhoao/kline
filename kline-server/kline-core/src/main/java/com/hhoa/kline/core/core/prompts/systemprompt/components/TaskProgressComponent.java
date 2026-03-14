package com.hhoa.kline.core.core.prompts.systemprompt.components;

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

        String template = getTemplateText();

        if (variant.getComponentOverrides() != null
                && variant.getComponentOverrides().containsKey(SystemPromptSection.TASK_PROGRESS)) {
            var override = variant.getComponentOverrides().get(SystemPromptSection.TASK_PROGRESS);
            if (override.getTemplate() != null) {
                template = override.getTemplate();
            }
        }

        return templateEngine.resolve(template, context, Map.of());
    }

    @Override
    public SystemPromptSection getSystemPromptSection() {
        return SystemPromptSection.TASK_PROGRESS;
    }

    private String getTemplateText() {
        return """
            UPDATING TASK PROGRESS

            Every tool use supports an optional task_progress parameter that allows you to provide an updated checklist to keep the user informed of your overall progress on the task. This should be used regularly throughout the task to keep the user informed of completed and remaining steps. Before using the attempt_completion tool, ensure the final checklist item is checked off to indicate task completion.

            - You probably wouldn't use this while in PLAN mode until the user has approved your plan and switched you to ACT mode.
            - Use standard Markdown checklist format: "- [ ]" for incomplete items and "- [x]" for completed items
            - Provide the whole checklist of steps you intend to complete in the task, and keep the checkboxes updated as you make progress. It's okay to rewrite this checklist as needed if it becomes invalid due to scope changes or new information.
            - Keep items focused on meaningful progress milestones rather than minor technical details. The checklist should not be so granular that minor implementation details clutter the progress tracking.
            - If you are creating this checklist for the first time, and the tool use completes the first step in the checklist, make sure to mark it as completed in your parameter input since this checklist will be displayed after this tool use is completed.
            - For simple tasks, short checklists with even a single item are acceptable. For complex tasks, avoid making the checklist too long or verbose.
            - If a checklist is being used, be sure to update it any time a step has been completed.

            Example:
            <execute_command>
            <command>npm install react</command>
            <requires_approval>false</requires_approval>
            <task_progress>
            - [x] Set up project structure
            - [x] Install dependencies
            - [ ] Create components
            - [ ] Test application
            </task_progress>
            </execute_command>
            """;
    }
}
