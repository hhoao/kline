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
public class PlanModeRespondTool extends BaseToolSpec {

    public static ClineToolSpec create(ModelFamily modelFamily, SystemPromptContext context) {
        List<ClineToolSpec.ClineToolSpecParameter> parameters = new ArrayList<>();
        parameters.add(
                createParameter(
                        "response",
                        true,
                        "The response to provide to the user. Do not try to use tools in this parameter, this is simply a chat response. (You MUST use the response parameter, do not simply place the response text directly within <plan_mode_respond> tags.)",
                        "Your response here"));
        parameters.add(
                createParameter(
                        "needs_more_exploration",
                        false,
                        "Set to true if while formulating your response that you found you need to do more exploration with tools, for example reading files. (Remember, you can explore the project with tools like read_file in PLAN MODE without the user having to toggle to ACT MODE.) Defaults to false if not specified.",
                        "true or false (optional, but you MUST set to true if in <response> you need to read files or use other exploration tools)"));

        if (context.getFocusChainSettings() != null
                && context.getFocusChainSettings().isEnabled()) {
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
                .description(
                        "Respond to the user's inquiry in an effort to plan a solution to the user's task. This tool should ONLY be used when you have already explored the relevant files and are ready to present a concrete plan. DO NOT use this tool to announce what files you're going to read - just read them first. This tool is only available in PLAN MODE. The environment_details will specify the current mode; if it is not PLAN_MODE then you should not use this tool.\n"
                                + "However, if while writing your response you realize you actually need to do more exploration before providing a complete plan, you can add the optional needs_more_exploration parameter to indicate this. This allows you to acknowledge that you should have done more exploration first, and signals that your next message will use exploration tools instead.")
                .parameters(parameters)
                .build();
    }
}
