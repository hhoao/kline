package com.hhoa.kline.core.core.prompts.systemprompt.tools;

import com.hhoa.kline.core.core.prompts.systemprompt.ClineToolSpec;
import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import com.hhoa.kline.core.enums.ClineDefaultTool;
import java.util.List;

/**
 * 尝试完成任务工具规格
 *
 * @author hhoa
 */
public class AttemptCompletionTool extends BaseToolSpec {

    public static ClineToolSpec create(ModelFamily modelFamily) {
        String description;
        if (modelFamily == ModelFamily.GPT_5) {
            description =
                    "After each tool use, the user will respond with the result of that tool use, i.e. if it succeeded or failed, along with any reasons for failure. Once you've received the results of tool uses and can confirm that the task is complete, use this tool to present the result of your work to the user. Optionally you may provide a CLI command to showcase the result of your work. The user may respond with feedback if they are not satisfied with the result, which you can use to make improvements and try again.\n"
                            + "IMPORTANT NOTE: This tool CANNOT be used until you've confirmed from the user that any previous tool uses were successful and all tasks have been completed in full. Failure to do so will result in code corruption and system failure. Before using this tool, you must ask yourself in <thinking></thinking> tags if you've confirmed from the user that any previous tool uses were successful and all goals defined by the user have been completed. If not, then DO NOT use this tool.";
        } else {
            description =
                    "After each tool use, the user will respond with the result of that tool use, i.e. if it succeeded or failed, along with any reasons for failure. Once you've received the results of tool uses and can confirm that the task is complete, use this tool to present the result of your work to the user. Optionally you may provide a CLI command to showcase the result of your work. The user may respond with feedback if they are not satisfied with the result, which you can use to make improvements and try again.\n"
                            + "IMPORTANT NOTE: This tool CANNOT be used until you've confirmed from the user that any previous tool uses were successful. Failure to do so will result in code corruption and system failure. Before using this tool, you must ask yourself in <thinking></thinking> tags if you've confirmed from the user that any previous tool uses were successful. If not, then DO NOT use this tool.";
        }

        return ClineToolSpec.builder()
                .variant(modelFamily)
                .id(ClineDefaultTool.ATTEMPT.getValue())
                .name(ClineDefaultTool.ATTEMPT.getValue())
                .description(description)
                .parameters(
                        List.of(
                                createParameter(
                                        "result",
                                        true,
                                        "The result of the tool use. This should be a clear, specific description of the result.",
                                        "Your final result description here"),
                                createParameter(
                                        "command",
                                        false,
                                        "A CLI command to execute to show a live demo of the result to the user. For example, use `open index.html` to display a created html website, or `open localhost:3000` to display a locally running development server. But DO NOT use commands like `echo` or `cat` that merely print text. This command should be valid for the current operating system. Ensure the command is properly formatted and does not contain any harmful instructions",
                                        "Your command here (optional)"),
                                createParameterWithDependency(
                                        "task_progress",
                                        false,
                                        "A checklist showing task progress after this tool use is completed. (See 'Updating Task Progress' section for more details)",
                                        "Checklist here (required if you used task_progress in previous tool uses)",
                                        "If you were using task_progress to update the task progress, you must include the completed list in the result as well.",
                                        ClineDefaultTool.TODO.getValue())))
                .build();
    }
}
