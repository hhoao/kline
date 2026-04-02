package com.hhoa.kline.core.core.prompts.systemprompt.tools;

import com.hhoa.kline.core.core.prompts.systemprompt.ClineToolSpec;
import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import com.hhoa.kline.core.enums.ClineDefaultTool;
import java.util.List;

/**
 * Web Search 工具规格 - 执行网络搜索并返回相关结果
 *
 * @author hhoa
 */
public class WebSearchTool extends BaseToolSpec
{

    private static final String GENERIC_DESCRIPTION =
            "Performs a web search and returns relevant results\n"
                    + "- Takes a search query as input and returns search results with titles and URLs\n"
                    + "- Optionally filter results by allowed or blocked domains\n"
                    + "- Use this tool when you need to search the web for information\n"
                    + "- IMPORTANT: If an MCP-provided web search tool is available, prefer using that tool "
                    + "instead of this one, as it may have fewer restrictions.\n"
                    + "- The query must be at least 2 characters\n"
                    + "- You may provide either allowed_domains OR blocked_domains, but NOT both\n"
                    + "- Domains should be provided as a JSON array of strings\n"
                    + "- This tool is read-only and does not modify any files";

    private static final String CONCISE_DESCRIPTION =
            "Performs a web search and returns relevant results with titles and URLs. "
                    + "IMPORTANT: If an MCP-provided web search tool is available, prefer using that tool "
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
                                    "query",
                                    true,
                                    "The search query to use",
                                    null),
                            createParameter(
                                    "allowed_domains",
                                    false,
                                    "JSON array of domains to restrict results to",
                                    null),
                            createParameter(
                                    "blocked_domains",
                                    false,
                                    "JSON array of domains to exclude from results",
                                    null),
                            createTaskProgressParameter());
        }
        else
        {
            parameters =
                    List.of(
                            createParameter(
                                    "query",
                                    true,
                                    "The search query to use",
                                    "latest developments in AI"),
                            createParameter(
                                    "allowed_domains",
                                    false,
                                    "JSON array of domains to restrict results to",
                                    "[\"example.com\", \"github.com\"]"),
                            createParameter(
                                    "blocked_domains",
                                    false,
                                    "JSON array of domains to exclude from results",
                                    "[\"ads.com\", \"spam.com\"]"),
                            createTaskProgressParameter());
        }

        return ClineToolSpec.builder()
                .variant(modelFamily)
                .id(ClineDefaultTool.WEB_SEARCH.getValue())
                .name(ClineDefaultTool.WEB_SEARCH.getValue())
                .description(description)
                .contextRequirements(
                        context ->
                                "cline".equals(context.getProviderId())
                                        && Boolean.TRUE.equals(context.getClineWebToolsEnabled()))
                .parameters(parameters)
                .build();
    }
}
