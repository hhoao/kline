package com.hhoa.kline.core.core.tools.specs;

import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import com.hhoa.kline.core.core.tools.ToolSpecProvider;
import com.hhoa.kline.core.core.tools.args.ExecuteCommandInput;
import com.hhoa.kline.core.core.tools.handlers.ExecuteCommandToolHandler;
import com.hhoa.kline.core.core.tools.handlers.ToolHandler;
import com.hhoa.kline.core.core.tools.ClineDefaultTool;

/** 执行命令工具规格。 */
public final class ExecuteCommandToolSpec implements ToolSpecProvider<ExecuteCommandInput> {

    private static final ExecuteCommandToolHandler HANDLER = new ExecuteCommandToolHandler();

    private static final String DESCRIPTION = "Execute a CLI command on the user's system.";

    private static final String GENERIC_PROMPT =
            "Request to execute a CLI command on the system. Use this when you need to perform system operations or run specific commands to accomplish any step in the user's task. You must tailor your command to the user's system and provide a clear explanation of what the command does. For command chaining, use the appropriate chaining syntax for the user's shell. Prefer to execute complex CLI commands over creating executable scripts, as they are more flexible and easier to run. Commands will be executed in the current working directory: {{CWD}}{{MULTI_ROOT_HINT}}";

    private static final String NATIVE_PROMPT =
            "Request to execute a CLI command on the system. Use this when you need to perform system operations or run specific commands to accomplish any step in the user's task.";

    private static final String GEMINI_3_PROMPT =
            "Request to execute a CLI command on the system. Use this when you need to perform system operations or run specific commands to accomplish any step in the user's task. When chaining commands, use the shell operator && (not the HTML entity &amp;&amp;). If using search/grep commands, be careful to not use vague search terms that may return thousands of results. When in PLAN MODE, you may use the execute_command tool, but only in a non-destructive manner and in a way that does not alter any files.";

    @Override
    public String name() {
        return ClineDefaultTool.BASH.getValue();
    }

    @Override
    public String description(ModelFamily family) {
        return DESCRIPTION;
    }

    @Override
    public String prompt(ModelFamily family) {
        return switch (family) {
            case GEMINI_3 -> GEMINI_3_PROMPT;
            case NATIVE_GPT_5, NATIVE_GPT_5_1, NATIVE_NEXT_GEN -> NATIVE_PROMPT;
            default -> GENERIC_PROMPT;
        };
    }

    @Override
    public Class<ExecuteCommandInput> inputType(ModelFamily family) {
        return ExecuteCommandInput.class;
    }

    @Override
    public ToolHandler<ExecuteCommandInput> handler(ModelFamily family) {
        return HANDLER;
    }
}
