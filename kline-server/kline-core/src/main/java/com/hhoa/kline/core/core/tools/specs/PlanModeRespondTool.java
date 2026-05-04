package com.hhoa.kline.core.core.tools.specs;

import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import com.hhoa.kline.core.core.tools.ToolSpecProvider;
import com.hhoa.kline.core.core.tools.args.PlanModeRespondInput;
import com.hhoa.kline.core.core.tools.handlers.PlanModeRespondHandler;
import com.hhoa.kline.core.core.tools.handlers.ToolHandler;
import com.hhoa.kline.core.core.tools.ClineDefaultTool;

/**
 * 计划模式响应工具规格
 *
 * @author hhoa
 */
public final class PlanModeRespondTool implements ToolSpecProvider<PlanModeRespondInput> {

    private static final PlanModeRespondHandler HANDLER = new PlanModeRespondHandler();

    private static final String DESCRIPTION = "Respond with a plan during PLAN MODE.";

    private static final String GENERIC_PROMPT =
            "Respond to the user's inquiry in an effort to plan a solution to the user's task. This tool should ONLY be used when you have already explored the relevant files and are ready to present a concrete plan. DO NOT use this tool to announce what files you're going to read - just read them first. This tool is only available in PLAN MODE. The environment_details will specify the current mode; if it is not PLAN_MODE then you should not use this tool.\n"
                    + "However, if while writing your response you realize you actually need to do more exploration before providing a complete plan, you can add the optional needs_more_exploration parameter to indicate this. This allows you to acknowledge that you should have done more exploration first, and signals that your next message will use exploration tools instead.";

    private static final String GEMINI_3_PROMPT =
            "Respond with a plan that outlines a solution to the user's request. This tool should ONLY be used when you have already explored the relevant files and are ready to present a concrete plan. Only use this tool after you have explored relevant files and collected sufficient context to create a detailed, accurate plan. This tool is only available in PLAN MODE, as indicated by the environment_details.\n"
                    + "If it becomes apparent that additional exploration is required while the plan_mode_respond response is being generated, the optional needs_more_exploration parameter can be toggled to enable further research. This allows you to acknowledge that more exploration is required before the final plan_mode_respond is generated, and signals that your next message will use exploration tools instead.";

    @Override
    public String name() {
        return ClineDefaultTool.PLAN_MODE.getValue();
    }

    @Override
    public String description(ModelFamily family) {
        return DESCRIPTION;
    }

    @Override
    public String prompt(ModelFamily family) {
        return family == ModelFamily.GEMINI_3 ? GEMINI_3_PROMPT : GENERIC_PROMPT;
    }

    @Override
    public Class<PlanModeRespondInput> inputType(ModelFamily family) {
        return PlanModeRespondInput.class;
    }

    @Override
    public ToolHandler<PlanModeRespondInput> handler(ModelFamily family) {
        return HANDLER;
    }
}
