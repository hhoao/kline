package com.hhoa.kline.core.core.tools.handlers;

import com.hhoa.kline.core.core.assistant.ToolUse;
import com.hhoa.kline.core.core.prompts.McpDocumentationLoader;
import com.hhoa.kline.core.core.shared.ClineSay;
import com.hhoa.kline.core.core.tools.args.LoadMcpDocumentationInput;
import com.hhoa.kline.core.core.tools.types.ToolContext;
import com.hhoa.kline.core.core.tools.types.ToolExecuteResult;
import com.hhoa.kline.core.core.tools.types.UIHelpers;
import java.util.ArrayList;
import java.util.List;

/** 加载 MCP 文档的工具处理器 */
public class LoadMcpDocumentationHandler implements ToolHandler<LoadMcpDocumentationInput> {
    @Override
    public String getDescription(ToolUse block) {
        return "[" + block.getName() + "]";
    }

    @Override
    public void handlePartialBlock(
            LoadMcpDocumentationInput input, ToolContext context, ToolUse block) {
        UIHelpers ui = UIHelpers.create(context);
        ui.say(ClineSay.LOAD_MCP_DOCUMENTATION, "", null, null, true, null);
    }

    @Override
    public ToolExecuteResult execute(
            LoadMcpDocumentationInput input, ToolContext context, ToolUse block) {
        context.getCallbacks().say(ClineSay.LOAD_MCP_DOCUMENTATION, "", null, null, false, null);

        try {
            if (context.getServices().getMcpHub() == null) {
                return HandlerUtils.createToolExecuteResult(
                        "Error loading MCP documentation: MCP Hub is not available.");
            }

            String mcpServersPath = "MCP servers directory";
            String mcpSettingsFilePath = "(application settings)";
            List<McpDocumentationLoader.McpServer> connectedServers = new ArrayList<>();

            McpDocumentationLoader loader = new McpDocumentationLoader();
            String documentation =
                    loader.loadMcpDocumentation(
                            mcpServersPath, mcpSettingsFilePath, connectedServers);

            return HandlerUtils.createToolExecuteResult(documentation);
        } catch (Exception error) {
            return HandlerUtils.createToolExecuteResult(
                    "Error loading MCP documentation: "
                            + (error.getMessage() != null ? error.getMessage() : "Unknown error"));
        }
    }
}
