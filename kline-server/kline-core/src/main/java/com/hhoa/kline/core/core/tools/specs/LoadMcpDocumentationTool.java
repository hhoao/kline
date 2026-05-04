package com.hhoa.kline.core.core.tools.specs;

import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptContext;
import com.hhoa.kline.core.core.tools.ToolSpecProvider;
import com.hhoa.kline.core.core.tools.args.LoadMcpDocumentationInput;
import com.hhoa.kline.core.core.tools.handlers.LoadMcpDocumentationHandler;
import com.hhoa.kline.core.core.tools.handlers.ToolHandler;
import com.hhoa.kline.core.core.tools.ClineDefaultTool;
import java.util.function.Function;

/**
 * 加载 MCP 文档工具规格
 *
 * @author hhoa
 */
public final class LoadMcpDocumentationTool implements ToolSpecProvider<LoadMcpDocumentationInput> {

    private static final LoadMcpDocumentationHandler HANDLER = new LoadMcpDocumentationHandler();

    private static final String DESCRIPTION = "Load documentation for creating MCP servers.";

    private static final String PROMPT =
            "Load documentation about creating MCP servers. This tool should be used when the user requests to create or install an MCP server (the user may ask you something along the lines of \"add a tool\" that does some function, in other words to create an MCP server that provides tools and resources that may connect to external APIs for example. You have the ability to create an MCP server and add it to a configuration file that will then expose the tools and resources for you to use with `use_mcp_tool` and `access_mcp_resource`). The documentation provides detailed information about the MCP server creation process, including setup instructions, best practices, and examples.";

    @Override
    public String name() {
        return ClineDefaultTool.MCP_DOCS.getValue();
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
        return context -> context.getMcpHub() != null;
    }

    @Override
    public Class<LoadMcpDocumentationInput> inputType(ModelFamily family) {
        return LoadMcpDocumentationInput.class;
    }

    @Override
    public ToolHandler<LoadMcpDocumentationInput> handler(ModelFamily family) {
        return HANDLER;
    }
}
