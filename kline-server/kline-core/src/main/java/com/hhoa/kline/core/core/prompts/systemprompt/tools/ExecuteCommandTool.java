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
public class ExecuteCommandTool extends BaseToolSpec
{

    private static final String GENERIC_DESCRIPTION =
            "Request to execute a CLI command on the system. Use this when you need to perform system operations or run specific commands to accomplish any step in the user's task. You must tailor your command to the user's system and provide a clear explanation of what the command does. For command chaining, use the appropriate chaining syntax for the user's shell. Prefer to execute complex CLI commands over creating executable scripts, as they are more flexible and easier to run. Commands will be executed in the current working directory: {{CWD}}{{MULTI_ROOT_HINT}}";

    private static final String NATIVE_GPT_5_DESCRIPTION =
            "Request to execute a CLI command on the system. Use this when you need to perform system operations or run specific commands to accomplish any step in the user's task.";

    private static final String GEMINI_3_DESCRIPTION =
            "Request to execute a CLI command on the system. Use this when you need to perform system operations or run specific commands to accomplish any step in the user's task. When chaining commands, use the shell operator && (not the HTML entity &amp;&amp;). If using search/grep commands, be careful to not use vague search terms that may return thousands of results. When in PLAN MODE, you may use the execute_command tool, but only in a non-destructive manner and in a way that does not alter any files.";

    public static ClineToolSpec create(ModelFamily modelFamily)
    {
        if (modelFamily == ModelFamily.NATIVE_GPT_5
                || modelFamily == ModelFamily.NATIVE_GPT_5_1
                || modelFamily == ModelFamily.NATIVE_NEXT_GEN)
        {
            return createNativeGpt5Variant(modelFamily);
        }

        if (modelFamily == ModelFamily.GEMINI_3)
        {
            return createGemini3Variant();
        }

        return createGenericVariant(modelFamily);
    }

    private static ClineToolSpec createGenericVariant(ModelFamily modelFamily)
    {
        List<ClineToolSpec.ClineToolSpecParameter> parameters = new ArrayList<>();
        parameters.add(
                createParameter(
                        "command",
                        true,
                        "The CLI command to execute. This should be valid for the current operating system. Ensure the command is properly formatted and does not contain any harmful instructions.",
                        "Your command here"));
        parameters.add(
                createParameterWithType(
                        "requires_approval",
                        true,
                        "A boolean indicating whether this command requires explicit user approval before execution in case the user has auto-approve mode enabled. Set to 'true' for potentially impactful operations like installing/uninstalling packages, deleting/overwriting files, system configuration changes, network operations, or any commands that could have unintended side effects. Set to 'false' for safe operations like reading files/directories, running development servers, building projects, and other non-destructive operations.",
                        "true or false",
                        "boolean"));

        Function<SystemPromptContext, Boolean> yoloModeContextRequirement =
                (context) -> Boolean.TRUE.equals(context.getYoloModeToggled());
        ClineToolSpec.ClineToolSpecParameter timeoutParam =
                ClineToolSpec.ClineToolSpecParameter.builder()
                        .name("timeout")
                        .required(false)
                        .type("integer")
                        .instruction(
                                "Integer representing the timeout in seconds for how long to run the terminal command, before timing out and continuing the task.")
                        .usage("30")
                        .contextRequirements(yoloModeContextRequirement)
                        .build();
        parameters.add(timeoutParam);

        return ClineToolSpec.builder()
                .variant(modelFamily)
                .id(ClineDefaultTool.BASH.getValue())
                .name(ClineDefaultTool.BASH.getValue())
                .description(GENERIC_DESCRIPTION)
                .parameters(parameters)
                .build();
    }

    private static ClineToolSpec createNativeGpt5Variant(ModelFamily modelFamily)
    {
        return ClineToolSpec.builder()
                .variant(modelFamily)
                .id(ClineDefaultTool.BASH.getValue())
                .name(ClineDefaultTool.BASH.getValue())
                .description(NATIVE_GPT_5_DESCRIPTION)
                .parameters(
                        List.of(
                                createParameter(
                                        "command",
                                        true,
                                        "The CLI command to execute. This should be valid for the current operating system. Do not use the ~ character or $HOME to refer to the home directory. Always use absolute paths. The command will be executed from the current workspace, you do not need to cd to the workspace.",
                                        null),
                                createParameterWithType(
                                        "requires_approval",
                                        true,
                                        "To indicate whether this command requires explicit user approval or interaction before it should be executed. For system/file altering operations like installing/uninstalling packages, removing/overwriting files, system configuration changes, network operations, or any commands that are considered potentially dangerous must be set to true. False for safe operations like running development servers, building projects, and other non-destructive operations.",
                                        null,
                                        "boolean")))
                .build();
    }

    private static ClineToolSpec createGemini3Variant()
    {
        return ClineToolSpec.builder()
                .variant(ModelFamily.GEMINI_3)
                .id(ClineDefaultTool.BASH.getValue())
                .name(ClineDefaultTool.BASH.getValue())
                .description(GEMINI_3_DESCRIPTION)
                .parameters(
                        List.of(
                                createParameter(
                                        "command",
                                        true,
                                        "The CLI command to execute. This should be valid for the current operating system. For command chaining, use proper shell operators like && to chain commands (e.g., 'cd path && command'). Do not use the ~ character or $HOME to refer to the home directory. Always use absolute paths. Do not run search/grep commands that may return thousands of results.",
                                        null),
                                createParameterWithType(
                                        "requires_approval",
                                        true,
                                        "To indicate whether this command requires explicit user approval or interaction before it should be executed. For system/file altering operations like installing/uninstalling packages, removing/overwriting files, system configuration changes, network operations, or any commands that are considered potentially dangerous must be set to true. False for safe operations like running development servers, building projects, and other non-destructive operations.",
                                        null,
                                        "boolean")))
                .build();
    }
}
