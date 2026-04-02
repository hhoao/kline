package com.hhoa.kline.core.core.prompts.systemprompt.tools;

import com.hhoa.kline.core.core.prompts.systemprompt.ClineToolSpec;
import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptContext;
import com.hhoa.kline.core.enums.ClineDefaultTool;
import java.util.ArrayList;
import java.util.List;

/**
 * 计划模式响应工具规格
 *
 * @author hhoa
 */
public class PlanModeRespondTool extends BaseToolSpec
{

    private static final String GENERIC_DESCRIPTION =
            "Respond to the user's inquiry in an effort to plan a solution to the user's task. This tool should ONLY be used when you have already explored the relevant files and are ready to present a concrete plan. DO NOT use this tool to announce what files you're going to read - just read them first. This tool is only available in PLAN MODE. The environment_details will specify the current mode; if it is not PLAN_MODE then you should not use this tool.\n"
                    + "However, if while writing your response you realize you actually need to do more exploration before providing a complete plan, you can add the optional needs_more_exploration parameter to indicate this. This allows you to acknowledge that you should have done more exploration first, and signals that your next message will use exploration tools instead.";

    private static final String GEMINI_3_DESCRIPTION =
            "Respond with a plan that outlines a solution to the user's request. This tool should ONLY be used when you have already explored the relevant files and are ready to present a concrete plan. Only use this tool after you have explored relevant files and collected sufficient context to create a detailed, accurate plan. This tool is only available in PLAN MODE, as indicated by the environment_details.\n"
                    + "If it becomes apparent that additional exploration is required while the plan_mode_respond response is being generated, the optional needs_more_exploration parameter can be toggled to enable further research. This allows you to acknowledge that more exploration is required before the final plan_mode_respond is generated, and signals that your next message will use exploration tools instead.";

    public static ClineToolSpec create(ModelFamily modelFamily, SystemPromptContext context)
    {
        if (modelFamily == ModelFamily.NATIVE_GPT_5
                || modelFamily == ModelFamily.NATIVE_GPT_5_1
                || modelFamily == ModelFamily.NATIVE_NEXT_GEN)
        {
            return createNativeGpt5Variant(modelFamily);
        }

        if (modelFamily == ModelFamily.GEMINI_3)
        {
            return createGemini3Variant(context);
        }

        return createGenericVariant(modelFamily, context);
    }

    private static ClineToolSpec createGenericVariant(
            ModelFamily modelFamily, SystemPromptContext context)
    {
        List<ClineToolSpec.ClineToolSpecParameter> parameters = new ArrayList<>();
        parameters.add(
                createParameter(
                        "response",
                        true,
                        "The response to provide to the user. Do not try to use tools in this parameter, this is simply a chat response. (You MUST use the response parameter, do not simply place the response text directly within <plan_mode_respond> tags.)",
                        "Your response here"));
        parameters.add(
                createParameterWithType(
                        "needs_more_exploration",
                        false,
                        "Set to true if while formulating your response that you found you need to do more exploration with tools, for example reading files. (Remember, you can explore the project with tools like read_file in PLAN MODE without the user having to toggle to ACT MODE.) Defaults to false if not specified.",
                        "true or false (optional, but you MUST set to true if in <response> you need to read files or use other exploration tools)",
                        "boolean"));

        if (context != null
                && context.getFocusChainSettings() != null
                && context.getFocusChainSettings().isEnabled())
        {
            parameters.add(
                    createParameterWithDependency(
                            "task_progress",
                            false,
                            " A checklist showing task progress after this tool use is completed. (See 'Updating Task Progress' section for more details)",
                            "Checklist here (If you have presented the user with concrete steps or requirements, you can optionally include a todo list outlining these steps.)",
                            null,
                            ClineDefaultTool.TODO.getValue()));
        }

        return ClineToolSpec.builder()
                .variant(modelFamily)
                .id(ClineDefaultTool.PLAN_MODE.getValue())
                .name(ClineDefaultTool.PLAN_MODE.getValue())
                .description(GENERIC_DESCRIPTION)
                .parameters(parameters)
                .build();
    }

    private static ClineToolSpec createNativeGpt5Variant(ModelFamily modelFamily)
    {
        return ClineToolSpec.builder()
                .variant(modelFamily)
                .id(ClineDefaultTool.PLAN_MODE.getValue())
                .name(ClineDefaultTool.PLAN_MODE.getValue())
                .description(GENERIC_DESCRIPTION)
                .parameters(
                        List.of(
                                createParameter(
                                        "response",
                                        true,
                                        "The response to provide to the user.",
                                        null),
                                createParameter(
                                        "task_progress",
                                        false,
                                        "A checklist showing task progress with the latest status of each subtasks included previously if any.",
                                        null)))
                .build();
    }

    private static ClineToolSpec createGemini3Variant(SystemPromptContext context)
    {
        List<ClineToolSpec.ClineToolSpecParameter> parameters = new ArrayList<>();
        parameters.add(
                createParameter(
                        "response",
                        true,
                        "A chat message response to the user.",
                        "Your response here"));
        parameters.add(
                createParameterWithType(
                        "needs_more_exploration",
                        false,
                        "needs_more_exploration can be set to true if it is determined that further exploration with read_file/search tools is necessary to formulate a complete plan. This determination can be reached during the response generation process, but should not be acknowledged until this parameter is set to true if required.",
                        "true or false (optional, but you MUST set to true if in <response> you need to read files or use other exploration tools)",
                        "boolean"));

        if (context != null
                && context.getFocusChainSettings() != null
                && context.getFocusChainSettings().isEnabled())
        {
            parameters.add(
                    createParameterWithDependency(
                            "task_progress",
                            false,
                            "A checklist showing task progress after this tool use is completed. If you are presenting a final implementation plan to the user with needs_more_exploration set to false, you should include a checklist of items to be completed during Act Mode when implementation is underway. (See 'Updating Task Progress' section for more details)",
                            "Checklist here (If you have presented the user with concrete steps or requirements, you can optionally include a todo list outlining these steps.)",
                            null,
                            ClineDefaultTool.TODO.getValue()));
        }

        return ClineToolSpec.builder()
                .variant(ModelFamily.GEMINI_3)
                .id(ClineDefaultTool.PLAN_MODE.getValue())
                .name(ClineDefaultTool.PLAN_MODE.getValue())
                .description(GEMINI_3_DESCRIPTION)
                .parameters(parameters)
                .build();
    }
}
