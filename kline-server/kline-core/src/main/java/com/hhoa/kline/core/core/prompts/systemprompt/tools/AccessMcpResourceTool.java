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

    private static final String GENERIC_DESCRIPTION =
            "Request to access a resource provided by a connected MCP server. Resources represent data sources that can be used as context, such as files, API responses, or system information.";

    private static final String NATIVE_DESCRIPTION =
            "Request to access a resource provided by a connected MCP server. Resources represent data sources that can be used as context, such as files, API responses, or system information. You must only use this tool if you have been informed of the MCP server and the resource you are trying to access.";

    private static final Function<SystemPromptContext, Boolean> CONTEXT_REQUIREMENTS =
            (context) -> context.getMcpHub() != null;

    public static ClineToolSpec create(ModelFamily modelFamily) {
        boolean isNative =
                modelFamily == ModelFamily.NATIVE_GPT_5
                        || modelFamily == ModelFamily.NATIVE_GPT_5_1
                        || modelFamily == ModelFamily.NATIVE_NEXT_GEN;

        String description = isNative ? NATIVE_DESCRIPTION : GENERIC_DESCRIPTION;

        return ClineToolSpec.builder()
                .variant(modelFamily)
                .id(ClineDefaultTool.MCP_ACCESS.getValue())
                .name(ClineDefaultTool.MCP_ACCESS.getValue())
                .description(description)
                .contextRequirements(CONTEXT_REQUIREMENTS)
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
