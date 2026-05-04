package com.hhoa.kline.core.core.tools.specs;

import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import com.hhoa.kline.core.core.tools.ToolSpecProvider;
import com.hhoa.kline.core.core.tools.args.NewTaskInput;
import com.hhoa.kline.core.core.tools.handlers.NewTaskHandler;
import com.hhoa.kline.core.core.tools.handlers.ToolHandler;
import com.hhoa.kline.core.core.tools.ClineDefaultTool;

/**
 * 新任务工具规格
 *
 * @author hhoa
 */
public final class NewTaskTool implements ToolSpecProvider<NewTaskInput> {

    private static final NewTaskHandler HANDLER = new NewTaskHandler();

    private static final String DESCRIPTION = "Create a new task with preloaded context.";

    private static final String PROMPT =
            "Request to create a new task with preloaded context covering the conversation with the user up to this point and key information for continuing with the new task. With this tool, you will create a detailed summary of the conversation so far, paying close attention to the user's explicit requests and your previous actions, with a focus on the most relevant information required for the new task.\n"
                    + "Among other important areas of focus, this summary should be thorough in capturing technical details, code patterns, and architectural decisions that would be essential for continuing with the new task. The user will be presented with a preview of your generated context and can choose to create a new task or keep chatting in the current conversation. The user may choose to start a new task at any point.";

    @Override
    public String name() {
        return ClineDefaultTool.NEW_TASK.getValue();
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
    public Class<NewTaskInput> inputType(ModelFamily family) {
        return NewTaskInput.class;
    }

    @Override
    public ToolHandler<NewTaskInput> handler(ModelFamily family) {
        return HANDLER;
    }
}
