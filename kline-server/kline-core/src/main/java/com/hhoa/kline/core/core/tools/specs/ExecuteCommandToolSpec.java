package com.hhoa.kline.core.core.tools.specs;

import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptContext;
import com.hhoa.kline.core.core.tools.ToolSpecProvider;
import com.hhoa.kline.core.core.tools.args.ExecuteCommandInput;
import com.hhoa.kline.core.core.tools.handlers.ExecuteCommandToolHandler;
import com.hhoa.kline.core.enums.ClineDefaultTool;
import java.util.Map;
import java.util.Set;

/** 执行命令工具规格。 */
public final class ExecuteCommandToolSpec extends BaseToolSpec
        implements ToolSpecProvider<ExecuteCommandInput, ExecuteCommandToolHandler> {

    private static final String GENERIC_DESCRIPTION =
            "Request to execute a CLI command on the system. Use this when you need to perform system operations or run specific commands to accomplish any step in the user's task. You must tailor your command to the user's system and provide a clear explanation of what the command does. For command chaining, use the appropriate chaining syntax for the user's shell. Prefer to execute complex CLI commands over creating executable scripts, as they are more flexible and easier to run. Commands will be executed in the current working directory: {{CWD}}{{MULTI_ROOT_HINT}}";

    private static final String NATIVE_DESCRIPTION =
            "Request to execute a CLI command on the system. Use this when you need to perform system operations or run specific commands to accomplish any step in the user's task.";

    private static final String GEMINI_3_DESCRIPTION =
            "Request to execute a CLI command on the system. Use this when you need to perform system operations or run specific commands to accomplish any step in the user's task. When chaining commands, use the shell operator && (not the HTML entity &amp;&amp;). If using search/grep commands, be careful to not use vague search terms that may return thousands of results. When in PLAN MODE, you may use the execute_command tool, but only in a non-destructive manner and in a way that does not alter any files.";

    @Override
    public String id() {
        return ClineDefaultTool.BASH.getValue();
    }

    @Override
    public String name() {
        return ClineDefaultTool.BASH.getValue();
    }

    @Override
    public String description(ModelFamily family) {
        return switch (family) {
            case GEMINI_3 -> GEMINI_3_DESCRIPTION;
            case NATIVE_GPT_5, NATIVE_GPT_5_1, NATIVE_NEXT_GEN -> NATIVE_DESCRIPTION;
            default -> GENERIC_DESCRIPTION;
        };
    }

    @Override
    public void customizeInputSchema(ModelFamily family, Map<String, Object> inputSchema) {
        if (family == ModelFamily.NATIVE_GPT_5
                || family == ModelFamily.NATIVE_GPT_5_1
                || family == ModelFamily.NATIVE_NEXT_GEN
                || family == ModelFamily.GEMINI_3) {
            return;
        }
        describe(
                inputSchema,
                "timeout",
                "Integer representing the timeout in seconds for how long to run the terminal command before timing out and continuing the task.");
        usage(inputSchema, "timeout", "30");
        contextRequirements(
                inputSchema,
                "timeout",
                (SystemPromptContext context) -> Boolean.TRUE.equals(context.getYoloModeToggled()));
    }

    @Override
    public Set<String> excludedParameters(ModelFamily family) {
        return switch (family) {
            case NATIVE_GPT_5, NATIVE_GPT_5_1, NATIVE_NEXT_GEN, GEMINI_3 -> Set.of("timeout");
            default -> Set.of();
        };
    }
}
