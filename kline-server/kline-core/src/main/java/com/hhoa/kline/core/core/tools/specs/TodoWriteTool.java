package com.hhoa.kline.core.core.tools.specs;

import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptContext;
import com.hhoa.kline.core.core.tools.ClineDefaultTool;
import com.hhoa.kline.core.core.tools.ToolSpecProvider;
import com.hhoa.kline.core.core.tools.args.TodoWriteInput;
import com.hhoa.kline.core.core.tools.handlers.TodoWriteToolHandler;
import com.hhoa.kline.core.core.tools.handlers.ToolHandler;
import java.util.function.Function;

public final class TodoWriteTool implements ToolSpecProvider<TodoWriteInput> {
    private static final TodoWriteToolHandler HANDLER = new TodoWriteToolHandler();

    private static final String DESCRIPTION =
            "Update the todo list for the current session.";

    private static final String PROMPT =
            """
            Use this tool to create and manage a structured task list for your current coding session. This helps you track progress, organize complex tasks, and demonstrate thoroughness to the user.
            It also helps the user understand the progress of the task and overall progress of their requests.

            Use this tool proactively for complex multi-step tasks, non-trivial implementation work, explicit user requests for a todo list, or when the user provides multiple tasks.

            Skip this tool when there is only a single straightforward task, the task is trivial, or the request is purely conversational or informational.

            Task states:
            - pending: Task not yet started
            - in_progress: Currently working on. Keep exactly one task in_progress when work is active.
            - completed: Task finished successfully

            Each task must include both:
            - content: imperative form describing what needs to be done, such as "Run tests"
            - activeForm: present continuous form shown during execution, such as "Running tests"

            Update the todo list in real time. Mark tasks completed immediately after finishing them, and remove tasks that are no longer relevant.
            """;

    @Override
    public String name() {
        return ClineDefaultTool.TODO.getValue();
    }

    @Override
    public String description(ModelFamily family) {
        return DESCRIPTION;
    }

    @Override
    public String prompt(ModelFamily family) {
        return PROMPT;
    }

    @Override
    public Function<SystemPromptContext, Boolean> contextRequirements(ModelFamily family) {
        return context ->
                context.getFocusChainSettings() != null
                        && Boolean.TRUE.equals(context.getFocusChainSettings().isEnabled());
    }

    @Override
    public Class<TodoWriteInput> inputType(ModelFamily family) {
        return TodoWriteInput.class;
    }

    @Override
    public ToolHandler<TodoWriteInput> handler(ModelFamily family) {
        return HANDLER;
    }
}
