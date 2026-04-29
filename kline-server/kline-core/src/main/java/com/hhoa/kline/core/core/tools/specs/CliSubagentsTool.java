package com.hhoa.kline.core.core.tools.specs;

import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import com.hhoa.kline.core.core.tools.ToolSpecProvider;
import com.hhoa.kline.core.core.tools.args.CliSubagentsInput;
import com.hhoa.kline.core.core.tools.handlers.ToolHandler;

/**
 * CLI 子代理工具规格
 *
 * @author hhoa
 */
public final class CliSubagentsTool extends BaseToolSpec
        implements ToolSpecProvider<CliSubagentsInput, ToolHandler> {

    private static final String ID = "cli_subagents";
    private static final String DESCRIPTION = "Use CLI subagents for focused tasks";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String description(ModelFamily family) {
        return DESCRIPTION;
    }
}
