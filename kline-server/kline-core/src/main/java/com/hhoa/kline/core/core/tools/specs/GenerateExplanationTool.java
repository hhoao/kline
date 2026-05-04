package com.hhoa.kline.core.core.tools.specs;

import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptContext;
import com.hhoa.kline.core.core.tools.ToolSpecProvider;
import com.hhoa.kline.core.core.tools.args.GenerateExplanationInput;
import com.hhoa.kline.core.core.tools.handlers.GenerateExplanationToolHandler;
import com.hhoa.kline.core.core.tools.handlers.ToolHandler;
import com.hhoa.kline.core.core.tools.ClineDefaultTool;
import java.util.function.Function;

/**
 * Generate Explanation 工具规格 - 生成 AI 驱动的代码变更解释
 *
 * @author hhoa
 */
public final class GenerateExplanationTool implements ToolSpecProvider<GenerateExplanationInput> {

    private static final GenerateExplanationToolHandler HANDLER = new GenerateExplanationToolHandler();

    private static final String DESCRIPTION = "Generate AI-powered comments for a multi-file diff.";

    private static final String PROMPT =
            "Opens a multi-file diff view and generates AI-powered inline comments explaining the changes "
                    + "between two git references. Use this tool to help users understand code changes from git commits, "
                    + "pull requests, branches, or any git refs. The tool uses git to retrieve file contents and "
                    + "displays a side-by-side diff view with explanatory comments.";

    @Override
    public String name() {
        return ClineDefaultTool.GENERATE_EXPLANATION.getValue();
    }

    @Override
    public String description(ModelFamily family) {
        return DESCRIPTION;
    }

    @Override
    public String prompt(ModelFamily family) {
        return PROMPT;
    }

    @Override
    public Function<SystemPromptContext, Boolean> contextRequirements(ModelFamily family) {
        return context -> !Boolean.TRUE.equals(context.getIsCliEnvironment());
    }

    @Override
    public Class<GenerateExplanationInput> inputType(ModelFamily family) {
        return GenerateExplanationInput.class;
    }

    @Override
    public ToolHandler<GenerateExplanationInput> handler(ModelFamily family) {
        return HANDLER;
    }
}
