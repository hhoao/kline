package com.hhoa.kline.core.core.task.tools.handlers;

import com.hhoa.kline.core.core.assistant.ToolUse;
import com.hhoa.kline.core.core.assistant.UserContentBlock;
import com.hhoa.kline.core.core.prompts.McpDocumentationLoader;
import com.hhoa.kline.core.core.prompts.systemprompt.ClineToolSpec;
import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import com.hhoa.kline.core.core.prompts.systemprompt.tools.LoadMcpDocumentationTool;
import com.hhoa.kline.core.core.shared.ClineSay;
import com.hhoa.kline.core.core.task.tools.types.TaskConfig;
import com.hhoa.kline.core.core.task.tools.types.UIHelpers;
import com.hhoa.kline.core.enums.ClineDefaultTool;
import java.util.ArrayList;
import java.util.List;

/** 加载 MCP 文档的工具处理器 */
public class LoadMcpDocumentationHandler implements FullyManagedTool {

    @Override
    public String getName() {
        return ClineDefaultTool.MCP_DOCS.getValue();
    }

    @Override
    public String getDescription(ToolUse block) {
        return "[" + block.getName() + "]";
    }

    @Override
    public ClineToolSpec getClineToolSpec() {
        return LoadMcpDocumentationTool.create(ModelFamily.GENERIC);
    }

    @Override
    public void handlePartialBlock(ToolUse block, UIHelpers ui) {
        ui.say(ClineSay.LOAD_MCP_DOCUMENTATION, "", null, null, true, null);
    }

    @Override
    public List<UserContentBlock> execute(TaskConfig config, ToolUse block) {
        config.getCallbacks().say(ClineSay.LOAD_MCP_DOCUMENTATION, "", null, null, false, null);
        config.getTaskState().setConsecutiveMistakeCount(0);

        try {
            if (config.getServices().getMcpHub() == null) {
                return HandlerUtils.createTextBlocks(
                        "Error loading MCP documentation: MCP Hub is not available.");
            }

            String mcpServersPath = "MCP servers directory";
            String mcpSettingsFilePath = "(application settings)";
            List<McpDocumentationLoader.McpServer> connectedServers = new ArrayList<>();

            McpDocumentationLoader loader = new McpDocumentationLoader();
            String documentation =
                    loader.loadMcpDocumentation(
                            mcpServersPath, mcpSettingsFilePath, connectedServers);

            return HandlerUtils.createTextBlocks(documentation);
        } catch (Exception error) {
            return HandlerUtils.createTextBlocks(
                    "Error loading MCP documentation: "
                            + (error.getMessage() != null ? error.getMessage() : "Unknown error"));
        }
    }
}
