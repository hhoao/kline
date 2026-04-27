package com.hhoa.kline.core.core.tools.specs;

import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptContext;
import com.hhoa.kline.core.core.tools.ToolSpec;
import com.hhoa.kline.core.enums.ClineDefaultTool;
import java.util.List;
import java.util.function.Function;

/**
 * 使用 MCP 工具工具规格
 *
 * @author hhoa
 */
public class UseMcpToolTool extends BaseToolSpec {

    public static ToolSpec create(ModelFamily modelFamily) {
        // 只有在 mcpHub 存在时才显示此工具
        Function<SystemPromptContext, Boolean> contextRequirements =
                (context) -> context.getMcpHub() != null;

        return ToolSpec.builder()
                .variant(modelFamily)
                .id(ClineDefaultTool.MCP_USE.getValue())
                .name(ClineDefaultTool.MCP_USE.getValue())
                .description(
                        "Request to use a tool provided by a connected MCP server. Each MCP server can provide multiple tools with different capabilities. Tools have defined input schemas that specify required and optional parameters.")
                .contextRequirements(contextRequirements)
                .parameters(
                        List.of(
                                createParameter(
                                        "server_name",
                                        true,
                                        "The name of the MCP server providing the tool",
                                        "server name here"),
                                createParameter(
                                        "tool_name",
                                        true,
                                        "The name of the tool to execute",
                                        "tool name here"),
                                createParameter(
                                        "arguments",
                                        true,
                                        "A JSON object containing the tool's input parameters, following the tool's input schema",
                                        "{\n"
                                                + "  \"param1\": \"value1\",\n"
                                                + "  \"param2\": \"value2\"\n"
                                                + "}"),
                                createTaskProgressParameter()))
                .build();
    }
}
