package com.hhoa.kline.core.core.prompts.systemprompt.tools;

import com.hhoa.kline.core.core.prompts.systemprompt.ClineToolSpec;
import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptContext;
import com.hhoa.kline.core.enums.ClineDefaultTool;
import java.util.Collections;
import java.util.function.Function;

/**
 * 加载 MCP 文档工具规格
 *
 * @author hhoa
 */
public class LoadMcpDocumentationTool extends BaseToolSpec {

    public static ClineToolSpec create(ModelFamily modelFamily) {
        // 只有在 mcpHub 存在时才显示此工具
        Function<SystemPromptContext, Boolean> contextRequirements =
                (context) -> context.getMcpHub() != null;

        return ClineToolSpec.builder()
                .variant(modelFamily)
                .id(ClineDefaultTool.MCP_DOCS.getValue())
                .name(ClineDefaultTool.MCP_DOCS.getValue())
                .description(
                        "Load documentation about creating MCP servers. This tool should be used when the user requests to create or install an MCP server (the user may ask you something along the lines of \"add a tool\" that does some function, in other words to create an MCP server that provides tools and resources that may connect to external APIs for example. You have the ability to create an MCP server and add it to a configuration file that will then expose the tools and resources for you to use with `use_mcp_tool` and `access_mcp_resource`). The documentation provides detailed information about the MCP server creation process, including setup instructions, best practices, and examples.")
                .contextRequirements(contextRequirements)
                .parameters(Collections.emptyList())
                .build();
    }
}
