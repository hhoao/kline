package com.hhoa.kline.core.core.prompts.systemprompt.tools;

import com.hhoa.kline.core.core.prompts.systemprompt.ClineToolSpec;
import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptContext;
import com.hhoa.kline.core.enums.ClineDefaultTool;
import java.util.List;
import java.util.function.Function;

/**
 * 访问 MCP 资源工具规格
 *
 * @author hhoa
 */
public class AccessMcpResourceTool extends BaseToolSpec {

    public static ClineToolSpec create(ModelFamily modelFamily) {
        Function<SystemPromptContext, Boolean> contextRequirements = (context) -> true;

        return ClineToolSpec.builder()
                .variant(modelFamily)
                .id(ClineDefaultTool.MCP_ACCESS.getValue())
                .name(ClineDefaultTool.MCP_ACCESS.getValue())
                .description(
                        "Request to access a resource provided by a connected MCP server. Resources represent data sources that can be used as context, such as files, API responses, or system information.")
                .contextRequirements(contextRequirements)
                .parameters(
                        List.of(
                                createParameter(
                                        "server_name",
                                        true,
                                        "The name of the MCP server providing the resource",
                                        "server name here"),
                                createParameter(
                                        "uri",
                                        true,
                                        "The URI identifying the specific resource to access",
                                        "resource URI here"),
                                createTaskProgressParameter()))
                .build();
    }
}
