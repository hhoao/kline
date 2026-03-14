package com.hhoa.kline.core.core.prompts.systemprompt.tools;

import com.hhoa.kline.core.core.prompts.systemprompt.ClineToolSpec;
import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptContext;
import com.hhoa.kline.core.enums.ClineDefaultTool;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * 执行命令工具规格
 *
 * @author hhoa
 */
public class ExecuteCommandTool extends BaseToolSpec {

    public static ClineToolSpec create(ModelFamily modelFamily) {
        // Note: Java version uses generic implementation for all model families
        // If GPT-specific handling is needed in the future, check model ID or provider info
        List<ClineToolSpec.ClineToolSpecParameter> parameters = new ArrayList<>();
        parameters.add(
                createParameter(
                        "command",
                        true,
                        "The CLI command to execute. This should be valid for the current operating system. Ensure the command is properly formatted and does not contain any harmful instructions.",
                        "Your command here"));
        parameters.add(
                createParameter(
                        "requires_approval",
                        true,
                        "A boolean indicating whether this command requires explicit user approval before execution in case the user has auto-approve mode enabled. Set to 'true' for potentially impactful operations like installing/uninstalling packages, deleting/overwriting files, system configuration changes, network operations, or any commands that could have unintended side effects. Set to 'false' for safe operations like reading files/directories, running development servers, building projects, and other non-destructive operations.",
                        "true or false"));

        // timeout 参数仅在 yoloModeToggled 为 true 时显示
        Function<SystemPromptContext, Boolean> yoloModeContextRequirement =
                (context) -> Boolean.TRUE.equals(context.getYoloModeToggled());
        ClineToolSpec.ClineToolSpecParameter timeoutParam =
                ClineToolSpec.ClineToolSpecParameter.builder()
                        .name("timeout")
                        .required(false)
                        .instruction(
                                "Integer representing the timeout in seconds for how long to run the terminal command, before timing out and continuing the task.")
                        .usage("30")
                        .contextRequirements(yoloModeContextRequirement)
                        .build();
        parameters.add(timeoutParam);

        // Note: generic 版本没有 task_progress 参数（只有 GPT 版本有）

        return ClineToolSpec.builder()
                .variant(modelFamily)
                .id(ClineDefaultTool.BASH.getValue())
                .name(ClineDefaultTool.BASH.getValue())
                .description(
                        "Request to execute a CLI command on the system. Use this when you need to perform system operations or run specific commands to accomplish any step in the user's task. You must tailor your command to the user's system and provide a clear explanation of what the command does. For command chaining, use the appropriate chaining syntax for the user's shell. Prefer to execute complex CLI commands over creating executable scripts, as they are more flexible and easier to run. Commands will be executed in the current working directory: {{CWD}}{{MULTI_ROOT_HINT}}")
                .parameters(parameters)
                .build();
    }
}
