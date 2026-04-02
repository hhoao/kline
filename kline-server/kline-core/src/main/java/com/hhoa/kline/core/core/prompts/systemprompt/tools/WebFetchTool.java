package com.hhoa.kline.core.core.prompts.systemprompt.tools;

import com.hhoa.kline.core.core.prompts.systemprompt.ClineToolSpec;
import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import com.hhoa.kline.core.enums.ClineDefaultTool;
import java.util.List;

/**
 * Web 获取工具规格
 *
 * @author hhoa
 */
public class WebFetchTool extends BaseToolSpec
{

    private static final String GENERIC_DESCRIPTION =
            "Fetches content from a specified URL and analyzes it using your prompt\n"
                    + "- Takes a URL and analysis prompt as input\n"
                    + "- Fetches the URL content and processes based on your prompt\n"
                    + "- Use this tool when you need to retrieve and analyze web content\n"
                    + "- IMPORTANT: If an MCP-provided web fetch tool is available, prefer using that tool instead of this one, as it may have fewer restrictions.\n"
                    + "- The URL must be a fully-formed valid URL\n"
                    + "- The prompt must be at least 2 characters\n"
                    + "- HTTP URLs will be automatically upgraded to HTTPS\n"
                    + "- This tool is read-only and does not modify any files";

    private static final String CONCISE_DESCRIPTION =
            "Fetches and analyzes content from a specified URL. "
                    + "IMPORTANT: If an MCP-provided web fetch tool is available, prefer using that tool "
                    + "instead of this one, as it may have fewer restrictions.";

    public static ClineToolSpec create(ModelFamily modelFamily)
    {
        boolean isNative =
                modelFamily == ModelFamily.NATIVE_GPT_5
                        || modelFamily == ModelFamily.NATIVE_NEXT_GEN;

        String description = isNative ? CONCISE_DESCRIPTION : GENERIC_DESCRIPTION;

        List<ClineToolSpec.ClineToolSpecParameter> parameters;
        if (isNative)
        {
            parameters =
                    List.of(
                            createParameter(
                                    "url",
                                    true,
                                    "The URL to fetch content from",
                                    null),
                            createParameter(
                                    "prompt",
                                    true,
                                    "Prompt for analyzing the webpage content",
                                    null),
                            createTaskProgressParameter());
        }
        else
        {
            parameters =
                    List.of(
                            createParameter(
                                    "url",
                                    true,
                                    "The URL to fetch content from",
                                    "https://example.com/docs"),
                            createParameter(
                                    "prompt",
                                    true,
                                    "The prompt to use for analyzing the webpage content",
                                    "Summarize the main points and key takeaways"),
                            createTaskProgressParameter());
        }

        return ClineToolSpec.builder()
                .variant(modelFamily)
                .id(ClineDefaultTool.WEB_FETCH.getValue())
                .name(ClineDefaultTool.WEB_FETCH.getValue())
                .description(description)
                .contextRequirements(
                        context ->
                                "cline".equals(context.getProviderId())
                                        && Boolean.TRUE.equals(context.getClineWebToolsEnabled()))
                .parameters(parameters)
                .build();
    }
}
