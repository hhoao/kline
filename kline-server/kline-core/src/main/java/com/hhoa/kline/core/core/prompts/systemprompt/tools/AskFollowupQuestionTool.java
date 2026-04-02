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
public class AskFollowupQuestionTool extends BaseToolSpec
{

    private static final String GENERIC_DESCRIPTION =
            "Ask the user a question to gather additional information needed to complete the task. This tool should be used when you encounter ambiguities, need clarification, or require more details to proceed effectively. It allows for interactive problem-solving by enabling direct communication with the user. Use this tool judiciously to maintain a balance between gathering necessary information and avoiding excessive back-and-forth.";

    private static final String NATIVE_DESCRIPTION =
            "Ask user a question for clarifying or gathering information needed to complete the task. For example, ask the user clarifying questions about a key implementation decision. You should only ask one question.";

    private static final Function<SystemPromptContext, Boolean> CONTEXT_REQUIREMENTS =
            (context) -> !Boolean.TRUE.equals(context.getYoloModeToggled());

    public static ClineToolSpec create(ModelFamily modelFamily)
    {
        if (modelFamily == ModelFamily.NATIVE_GPT_5
                || modelFamily == ModelFamily.NATIVE_GPT_5_1
                || modelFamily == ModelFamily.NATIVE_NEXT_GEN)
        {
            return createNativeVariant(modelFamily);
        }

        return createGenericVariant(modelFamily);
    }

    private static ClineToolSpec createGenericVariant(ModelFamily modelFamily)
    {
        return ClineToolSpec.builder()
                .variant(modelFamily)
                .id(ClineDefaultTool.ASK.getValue())
                .name(ClineDefaultTool.ASK.getValue())
                .description(GENERIC_DESCRIPTION)
                .contextRequirements(CONTEXT_REQUIREMENTS)
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

    private static ClineToolSpec createNativeVariant(ModelFamily modelFamily)
    {
        return ClineToolSpec.builder()
                .variant(modelFamily)
                .id(ClineDefaultTool.ASK.getValue())
                .name(ClineDefaultTool.ASK.getValue())
                .description(NATIVE_DESCRIPTION)
                .contextRequirements(CONTEXT_REQUIREMENTS)
                .parameters(
                        List.of(
                                createParameter(
                                        "question",
                                        true,
                                        "The single question to ask the user. E.g. \"How can I help you?\"",
                                        null),
                                createParameter(
                                        "options",
                                        true,
                                        "An array of 2-5 options (e.x: \"[\"Option 1\", \"Option 2\", \"Option 3\"]\") for the user to choose from. Each option should be a string describing a possible answer to the single question. You may not always need to provide options, but it may be helpful in many cases where it can save the user from having to type out a response manually. IMPORTANT: NEVER include an option to toggle to Act mode, as this would be something you need to direct the user to do manually themselves if needed.",
                                        null),
                                createTaskProgressParameter()))
                .build();
    }
}
