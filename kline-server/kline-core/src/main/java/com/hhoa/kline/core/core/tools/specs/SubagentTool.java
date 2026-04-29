package com.hhoa.kline.core.core.tools.specs;

import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptContext;
import com.hhoa.kline.core.core.tools.ToolSpecProvider;
import com.hhoa.kline.core.core.tools.args.SubagentInput;
import com.hhoa.kline.core.core.tools.handlers.SubagentToolHandler;
import com.hhoa.kline.core.enums.ClineDefaultTool;
import java.util.function.Function;

/**
 * Subagent 工具规格 - 运行最多5个并行子代理进行研究
 *
 * @author hhoa
 */
public final class SubagentTool extends BaseToolSpec
        implements ToolSpecProvider<SubagentInput, SubagentToolHandler> {

    private static final String DESCRIPTION =
            "Run up to five focused in-process subagents in parallel. Each subagent gets its own prompt "
                    + "and returns a comprehensive research result with tool and token stats. Use this for broad "
                    + "exploration when reading many files would consume the main agent's context window. You do not "
                    + "need to launch multiple subagents every time; using one subagent is valid when it avoids "
                    + "unnecessary context usage for light discovery work.";

    @Override
    public String id() {
        return ClineDefaultTool.USE_SUBAGENTS.getValue();
    }

    @Override
    public String description(ModelFamily family) {
        return DESCRIPTION;
    }

    @Override
    public Function<SystemPromptContext, Boolean> contextRequirements(ModelFamily family) {
        return context ->
                Boolean.TRUE.equals(context.getSubagentsEnabled())
                        && !Boolean.TRUE.equals(context.getIsSubagentRun());
    }
}
