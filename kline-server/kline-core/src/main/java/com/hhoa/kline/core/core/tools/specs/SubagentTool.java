package com.hhoa.kline.core.core.tools.specs;

import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import com.hhoa.kline.core.core.tools.ToolSpec;
import com.hhoa.kline.core.enums.ClineDefaultTool;
import java.util.List;

/**
 * Subagent 工具规格 - 运行最多5个并行子代理进行研究
 *
 * @author hhoa
 */
public class SubagentTool extends BaseToolSpec {

    public static ToolSpec create(ModelFamily modelFamily) {
        return ToolSpec.builder()
                .variant(modelFamily)
                .id(ClineDefaultTool.USE_SUBAGENTS.getValue())
                .name(ClineDefaultTool.USE_SUBAGENTS.getValue())
                .description(
                        "Run up to five focused in-process subagents in parallel. Each subagent gets its own prompt "
                                + "and returns a comprehensive research result with tool and token stats. Use this for broad "
                                + "exploration when reading many files would consume the main agent's context window. You do not "
                                + "need to launch multiple subagents every time; using one subagent is valid when it avoids "
                                + "unnecessary context usage for light discovery work.")
                .contextRequirements(
                        context ->
                                Boolean.TRUE.equals(context.getSubagentsEnabled())
                                        && !Boolean.TRUE.equals(context.getIsSubagentRun()))
                .parameters(
                        List.of(
                                createParameter("prompt_1", true, "First subagent prompt.", null),
                                createParameter(
                                        "prompt_2",
                                        false,
                                        "Optional second subagent prompt.",
                                        null),
                                createParameter(
                                        "prompt_3", false, "Optional third subagent prompt.", null),
                                createParameter(
                                        "prompt_4",
                                        false,
                                        "Optional fourth subagent prompt.",
                                        null),
                                createParameter(
                                        "prompt_5",
                                        false,
                                        "Optional fifth subagent prompt.",
                                        null)))
                .build();
    }
}
