package com.hhoa.kline.core.core.tools.specs;

import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import com.hhoa.kline.core.core.tools.ToolSpecProvider;
import com.hhoa.kline.core.core.tools.args.AttemptCompletionInput;
import com.hhoa.kline.core.core.tools.handlers.AttemptCompletionHandler;
import com.hhoa.kline.core.enums.ClineDefaultTool;
import java.util.Set;

/**
 * 尝试完成任务工具规格
 *
 * @author hhoa
 */
public final class AttemptCompletionTool extends BaseToolSpec
        implements ToolSpecProvider<AttemptCompletionInput, AttemptCompletionHandler> {

    private static final String GENERIC_DESCRIPTION =
            "After each tool use, the user will respond with the result of that tool use, i.e. if it succeeded or failed, along with any reasons for failure. Once you've received the results of tool uses and can confirm that the task is complete, use this tool to present the result of your work to the user. Optionally you may provide a CLI command to showcase the result of your work. The user may respond with feedback if they are not satisfied with the result, which you can use to make improvements and try again.\n"
                    + "IMPORTANT NOTE: This tool CANNOT be used until you've confirmed from the user that any previous tool uses were successful. Failure to do so will result in code corruption and system failure. Before using this tool, you must ask yourself in <thinking></thinking> tags if you've confirmed from the user that any previous tool uses were successful. If not, then DO NOT use this tool.";

    private static final String GPT_5_DESCRIPTION =
            "After each tool use, the user will respond with the result of that tool use, i.e. if it succeeded or failed, along with any reasons for failure. Once you've received the results of tool uses and can confirm that the task is complete, use this tool to present the result of your work to the user. Optionally you may provide a CLI command to showcase the result of your work. The user may respond with feedback if they are not satisfied with the result, which you can use to make improvements and try again.\n"
                    + "IMPORTANT NOTE: This tool CANNOT be used until you've confirmed from the user that any previous tool uses were successful and all tasks have been completed in full. Failure to do so will result in code corruption and system failure. Before using this tool, you must ask yourself in <thinking></thinking> tags if you've confirmed from the user that any previous tool uses were successful and all goals defined by the user have been completed. If not, then DO NOT use this tool.";

    private static final String NATIVE_DESCRIPTION =
            "Once you've completed the user's task, use this tool to present the final result to the user, including a brief and very short (1-2 paragraph) summary of the task and what was done to resolve it. Provide the basics, hitting the highlights, but do delve into the specifics. You should only call this tool when you have completed all tasks in the task_progress list, and completed all changes that are necessary to satisfy the user's request. You should not provide the contents of the task_progress list in the result parameter, it must be included in the task_progress parameter.";

    @Override
    public String id() {
        return ClineDefaultTool.ATTEMPT.getValue();
    }

    @Override
    public String description(ModelFamily family) {
        return switch (family) {
            case NATIVE_GPT_5, NATIVE_GPT_5_1, NATIVE_NEXT_GEN -> NATIVE_DESCRIPTION;
            case GPT_5 -> GPT_5_DESCRIPTION;
            default -> GENERIC_DESCRIPTION;
        };
    }

    @Override
    public Set<String> excludedParameters(ModelFamily family) {
        return switch (family) {
            case NATIVE_GPT_5, NATIVE_GPT_5_1, NATIVE_NEXT_GEN -> Set.of("command");
            default -> Set.of();
        };
    }
}
