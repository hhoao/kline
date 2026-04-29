package com.hhoa.kline.core.core.tools.specs;

import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptContext;
import com.hhoa.kline.core.core.tools.ToolSpecProvider;
import com.hhoa.kline.core.core.tools.args.AskFollowupQuestionInput;
import com.hhoa.kline.core.core.tools.handlers.AskFollowupQuestionToolHandler;
import com.hhoa.kline.core.enums.ClineDefaultTool;
import java.util.Set;
import java.util.function.Function;

/**
 * 询问后续问题工具规格
 *
 * @author hhoa
 */
public final class AskFollowupQuestionTool extends BaseToolSpec
        implements ToolSpecProvider<AskFollowupQuestionInput, AskFollowupQuestionToolHandler> {

    private static final String GENERIC_DESCRIPTION =
            "Ask the user a question to gather additional information needed to complete the task. This tool should be used when you encounter ambiguities, need clarification, or require more details to proceed effectively. It allows for interactive problem-solving by enabling direct communication with the user. Use this tool judiciously to maintain a balance between gathering necessary information and avoiding excessive back-and-forth.";

    private static final String NATIVE_DESCRIPTION =
            "Ask user a question for clarifying or gathering information needed to complete the task. For example, ask the user clarifying questions about a key implementation decision. You should only ask one question.";

    private static final Function<SystemPromptContext, Boolean> CONTEXT_REQUIREMENTS =
            (context) -> !Boolean.TRUE.equals(context.getYoloModeToggled());

    @Override
    public String id() {
        return ClineDefaultTool.ASK.getValue();
    }

    @Override
    public String description(ModelFamily family) {
        return switch (family) {
            case NATIVE_GPT_5, NATIVE_GPT_5_1, NATIVE_NEXT_GEN -> NATIVE_DESCRIPTION;
            default -> GENERIC_DESCRIPTION;
        };
    }

    @Override
    public Function<SystemPromptContext, Boolean> contextRequirements(ModelFamily family) {
        return CONTEXT_REQUIREMENTS;
    }

    @Override
    public Set<String> excludedParameters(ModelFamily family) {
        return switch (family) {
            case NATIVE_GPT_5, NATIVE_GPT_5_1, NATIVE_NEXT_GEN -> Set.of("options");
            default -> Set.of();
        };
    }
}
