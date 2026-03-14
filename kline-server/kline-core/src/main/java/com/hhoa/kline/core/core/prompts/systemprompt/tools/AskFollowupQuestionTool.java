package com.hhoa.kline.core.core.prompts.systemprompt.tools;

import com.hhoa.kline.core.core.prompts.systemprompt.ClineToolSpec;
import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptContext;
import com.hhoa.kline.core.enums.ClineDefaultTool;
import java.util.List;
import java.util.function.Function;

/**
 * 询问后续问题工具规格
 *
 * @author hhoa
 */
public class AskFollowupQuestionTool extends BaseToolSpec {

    public static ClineToolSpec create(ModelFamily modelFamily) {
        // 只有在 yoloModeToggled 为 false 时才显示此工具
        Function<SystemPromptContext, Boolean> contextRequirements =
                (context) -> !Boolean.TRUE.equals(context.getYoloModeToggled());

        return ClineToolSpec.builder()
                .variant(modelFamily)
                .id(ClineDefaultTool.ASK.getValue())
                .name(ClineDefaultTool.ASK.getValue())
                .description(
                        "Ask the user a question to gather additional information needed to complete the task. This tool should be used when you encounter ambiguities, need clarification, or require more details to proceed effectively. It allows for interactive problem-solving by enabling direct communication with the user. Use this tool judiciously to maintain a balance between gathering necessary information and avoiding excessive back-and-forth.")
                .contextRequirements(contextRequirements)
                .parameters(
                        List.of(
                                createParameter(
                                        "question",
                                        true,
                                        "The question to ask the user. This should be a clear, specific question that addresses the information you need.",
                                        "Your question here"),
                                createParameter(
                                        "options",
                                        false,
                                        "An array of 2-5 options for the user to choose from. Each option should be a string describing a possible answer. You may not always need to provide options, but it may be helpful in many cases where it can save the user from having to type out a response manually. IMPORTANT: NEVER include an option to toggle to Act mode, as this would be something you need to direct the user to do manually themselves if needed.",
                                        "Array of options here (optional), e.g. [\"Option 1\", \"Option 2\", \"Option 3\"]"),
                                createTaskProgressParameter()))
                .build();
    }
}
