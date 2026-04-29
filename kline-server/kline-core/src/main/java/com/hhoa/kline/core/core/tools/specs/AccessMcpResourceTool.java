package com.hhoa.kline.core.core.tools.specs;

import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptContext;
import com.hhoa.kline.core.core.tools.ToolSpecProvider;
import com.hhoa.kline.core.core.tools.args.AccessMcpResourceInput;
import com.hhoa.kline.core.core.tools.handlers.AccessMcpResourceHandler;
import com.hhoa.kline.core.enums.ClineDefaultTool;
import java.util.function.Function;

/**
 * 访问 MCP 资源工具规格
 *
 * @author hhoa
 */
public final class AccessMcpResourceTool extends BaseToolSpec
        implements ToolSpecProvider<AccessMcpResourceInput, AccessMcpResourceHandler> {

    private static final String GENERIC_DESCRIPTION =
            "Request to access a resource provided by a connected MCP server. Resources represent data sources that can be used as context, such as files, API responses, or system information.";

    private static final String NATIVE_DESCRIPTION =
            "Request to access a resource provided by a connected MCP server. Resources represent data sources that can be used as context, such as files, API responses, or system information. You must only use this tool if you have been informed of the MCP server and the resource you are trying to access.";

    private static final Function<SystemPromptContext, Boolean> CONTEXT_REQUIREMENTS =
            (context) -> context.getMcpHub() != null;

    @Override
    public String id() {
        return ClineDefaultTool.MCP_ACCESS.getValue();
    }

    @Override
    public String description(ModelFamily family) {
        return switch (family) {
            case NATIVE_GPT_5, NATIVE_GPT_5_1, NATIVE_NEXT_GEN -> NATIVE_DESCRIPTION;
            default -> GENERIC_DESCRIPTION;
        };
    }

    @Override
    public Function<SystemPromptContext, Boolean> contextRequirements(ModelFamily family) {
        return CONTEXT_REQUIREMENTS;
    }
}
