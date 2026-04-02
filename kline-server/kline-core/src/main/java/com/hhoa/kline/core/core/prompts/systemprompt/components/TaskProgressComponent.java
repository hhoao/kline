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
        return """
            UPDATING TASK PROGRESS

            You can track and communicate your progress on the overall task using the task_progress parameter supported by every tool call. Using task_progress ensures you remain on task, and stay focused on completing the user's objective. This parameter can be used in any mode, and with any tool call.

            - When switching from PLAN MODE to ACT MODE, you must create a comprehensive todo list for the task using the task_progress parameter
            - Todo list updates should be done silently using the task_progress parameter - do not announce these updates to the user
            - Keep items focused on meaningful progress milestones rather than minor technical details. The checklist should not be so granular that minor implementation details clutter the progress tracking.
            - For simple tasks, short checklists with even a single item are acceptable. For complex tasks, avoid making the checklist too long or verbose.
            - If you are creating this checklist for the first time, and the tool use completes the first step in the checklist, make sure to mark it as completed in your task_progress parameter.
            - Provide the whole checklist of steps you intend to complete in the task, and keep the checkboxes updated as you make progress. It's okay to rewrite this checklist as needed if it becomes invalid due to scope changes or new information.
            - If a checklist is being used, be sure to update it any time a step has been completed.
            - The system will automatically include todo list context in your prompts when appropriate - these reminders are important.

            **How to use task_progress:**
            - include the task_progress parameter in your tool calls to provide an updated checklist
            - Use standard Markdown checklist format: "- [ ]" for incomplete items and "- [x]" for completed items
            - The task_progress parameter MUST be included as a separate parameter in the tool, it should not be included inside other content or argument blocks.
            """;
    }

    private String getNativeGpt5TemplateText() {
        return """
            UPDATING TASK PROGRESS

            You can track and communicate your progress on the overall task using the task_progress parameter supported by every tool call. Using task_progress ensures you remain on task, and stay focused on completing the user's objective. This parameter can be used in any mode, and with any tool call.

            - When switching from PLAN MODE to ACT MODE, you MUST create a comprehensive todo list for the task using the task_progress parameter
            - Todo list updates should be done silently using the task_progress parameter, without announcing these updates to the user through content parameters
            - Keep items focused on meaningful progress milestones rather than minor technical details. The checklist should avoid being so granular that minor implementation details clutter the progress tracking.
            - For simple tasks, short checklists with even a single item are acceptable.
            - If you are creating this checklist for the first time, and the tool use completes the first step in the checklist, make sure to mark it as completed in your task_progress parameter.
            - Provide the whole checklist of steps you intend to complete in the task, and keep the checkboxes updated as you make progress. It's okay to rewrite this checklist as needed if it becomes invalid due to scope changes or new information.
            - Be sure to update the list any time a step has been completed.
            - The system may include todo list context in your prompts when appropriate - these reminders are important, and serve as a validation of your successful task execution.

            **How to use task_progress:**
            - include the task_progress parameter in your tool calls to provide an updated checklist
            - Use standard Markdown checklist format: "- [ ]" for incomplete items and "- [x]" for completed items
            - The task_progress parameter MUST be included as a separate parameter in the tool, it should NOT be included inside other content or argument blocks.
            """;
    }

    private String getTemplateText() {
        return """
            UPDATING TASK PROGRESS

            You can track and communicate your progress on the overall task using the task_progress parameter supported by every tool call. Using task_progress ensures you remain on task, and stay focused on completing the user's objective. This parameter can be used in any mode, and with any tool call.

            - When switching from PLAN MODE to ACT MODE, you must create a comprehensive todo list for the task using the task_progress parameter
            - Todo list updates should be done silently using the task_progress parameter - do not announce these updates to the user
            - Use standard Markdown checklist format: "- [ ]" for incomplete items and "- [x]" for completed items
            - Keep items focused on meaningful progress milestones rather than minor technical details. The checklist should not be so granular that minor implementation details clutter the progress tracking.
            - For simple tasks, short checklists with even a single item are acceptable. For complex tasks, avoid making the checklist too long or verbose.
            - If you are creating this checklist for the first time, and the tool use completes the first step in the checklist, make sure to mark it as completed in your task_progress parameter.
            - Provide the whole checklist of steps you intend to complete in the task, and keep the checkboxes updated as you make progress. It's okay to rewrite this checklist as needed if it becomes invalid due to scope changes or new information.
            - If a checklist is being used, be sure to update it any time a step has been completed.
            - The system will automatically include todo list context in your prompts when appropriate - these reminders are important.

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
