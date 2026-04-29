package com.hhoa.kline.core.core.tools.specs;

import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptContext;
import com.hhoa.kline.core.core.tools.ToolSpecProvider;
import com.hhoa.kline.core.core.tools.args.UseMcpToolInput;
import com.hhoa.kline.core.core.tools.handlers.UseMcpToolHandler;
import com.hhoa.kline.core.enums.ClineDefaultTool;
import java.util.function.Function;

/**
 * 使用 MCP 工具工具规格
 *
 * @author hhoa
 */
public final class UseMcpToolTool extends BaseToolSpec
        implements ToolSpecProvider<UseMcpToolInput, UseMcpToolHandler> {

    private static final String DESCRIPTION =
            "Request to use a tool provided by a connected MCP server. Each MCP server can provide multiple tools with different capabilities. Tools have defined input schemas that specify required and optional parameters.";

    @Override
    public String id() {
        return ClineDefaultTool.MCP_USE.getValue();
    }

    @Override
    public String description(ModelFamily family) {
        return DESCRIPTION;
    }

    @Override
    public Function<SystemPromptContext, Boolean> contextRequirements(ModelFamily family) {
        return context -> context.getMcpHub() != null;
    }
}
