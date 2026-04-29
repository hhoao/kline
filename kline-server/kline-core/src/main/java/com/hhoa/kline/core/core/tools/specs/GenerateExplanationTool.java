package com.hhoa.kline.core.core.tools.specs;

import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptContext;
import com.hhoa.kline.core.core.tools.ToolSpecProvider;
import com.hhoa.kline.core.core.tools.args.GenerateExplanationInput;
import com.hhoa.kline.core.core.tools.handlers.GenerateExplanationToolHandler;
import com.hhoa.kline.core.enums.ClineDefaultTool;
import java.util.function.Function;

/**
 * Generate Explanation 工具规格 - 生成 AI 驱动的代码变更解释
 *
 * @author hhoa
 */
public final class GenerateExplanationTool extends BaseToolSpec
        implements ToolSpecProvider<GenerateExplanationInput, GenerateExplanationToolHandler> {

    private static final String DESCRIPTION =
            "Opens a multi-file diff view and generates AI-powered inline comments explaining the changes "
                    + "between two git references. Use this tool to help users understand code changes from git commits, "
                    + "pull requests, branches, or any git refs. The tool uses git to retrieve file contents and "
                    + "displays a side-by-side diff view with explanatory comments.";

    @Override
    public String id() {
        return ClineDefaultTool.GENERATE_EXPLANATION.getValue();
    }

    @Override
    public String description(ModelFamily family) {
        return DESCRIPTION;
    }

    @Override
    public Function<SystemPromptContext, Boolean> contextRequirements(ModelFamily family) {
        return context -> !Boolean.TRUE.equals(context.getIsCliEnvironment());
    }
}
