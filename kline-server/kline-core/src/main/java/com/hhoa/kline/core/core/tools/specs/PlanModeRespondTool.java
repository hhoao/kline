package com.hhoa.kline.core.core.tools.specs;

import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import com.hhoa.kline.core.core.tools.ToolSpecProvider;
import com.hhoa.kline.core.core.tools.args.PlanModeRespondInput;
import com.hhoa.kline.core.core.tools.handlers.PlanModeRespondHandler;
import com.hhoa.kline.core.enums.ClineDefaultTool;
import java.util.Map;
import java.util.Set;

/**
 * 计划模式响应工具规格
 *
 * @author hhoa
 */
public final class PlanModeRespondTool extends BaseToolSpec
        implements ToolSpecProvider<PlanModeRespondInput, PlanModeRespondHandler> {

    private static final String GENERIC_DESCRIPTION =
            "Respond to the user's inquiry in an effort to plan a solution to the user's task. This tool should ONLY be used when you have already explored the relevant files and are ready to present a concrete plan. DO NOT use this tool to announce what files you're going to read - just read them first. This tool is only available in PLAN MODE. The environment_details will specify the current mode; if it is not PLAN_MODE then you should not use this tool.\n"
                    + "However, if while writing your response you realize you actually need to do more exploration before providing a complete plan, you can add the optional needs_more_exploration parameter to indicate this. This allows you to acknowledge that you should have done more exploration first, and signals that your next message will use exploration tools instead.";

    private static final String GEMINI_3_DESCRIPTION =
            "Respond with a plan that outlines a solution to the user's request. This tool should ONLY be used when you have already explored the relevant files and are ready to present a concrete plan. Only use this tool after you have explored relevant files and collected sufficient context to create a detailed, accurate plan. This tool is only available in PLAN MODE, as indicated by the environment_details.\n"
                    + "If it becomes apparent that additional exploration is required while the plan_mode_respond response is being generated, the optional needs_more_exploration parameter can be toggled to enable further research. This allows you to acknowledge that more exploration is required before the final plan_mode_respond is generated, and signals that your next message will use exploration tools instead.";

    @Override
    public String id() {
        return ClineDefaultTool.PLAN_MODE.getValue();
    }

    @Override
    public String description(ModelFamily family) {
        return family == ModelFamily.GEMINI_3 ? GEMINI_3_DESCRIPTION : GENERIC_DESCRIPTION;
    }

    @Override
    public void customizeInputSchema(ModelFamily family, Map<String, Object> inputSchema) {
        if (family == ModelFamily.GEMINI_3) {
            require(inputSchema, "response");
            optional(inputSchema, "needs_more_exploration");
            optional(inputSchema, "options");
            describe(inputSchema, "response", "The plan response to provide to the user.");
            describe(
                    inputSchema,
                    "needs_more_exploration",
                    "Set true when additional exploration is required before the final plan.");
            describe(
                    inputSchema,
                    "options",
                    "Options for the user to choose from, if useful for the plan.");
            return;
        }
        if (family == ModelFamily.NATIVE_GPT_5
                || family == ModelFamily.NATIVE_GPT_5_1
                || family == ModelFamily.NATIVE_NEXT_GEN) {
            require(inputSchema, "response");
            describe(inputSchema, "response", "The response to provide to the user.");
        }
    }

    @Override
    public Set<String> excludedParameters(ModelFamily family) {
        return switch (family) {
            case NATIVE_GPT_5, NATIVE_GPT_5_1, NATIVE_NEXT_GEN ->
                    Set.of("needs_more_exploration", "options", "task_progress");
            case GEMINI_3 -> Set.of("task_progress");
            default -> Set.of();
        };
    }
}
