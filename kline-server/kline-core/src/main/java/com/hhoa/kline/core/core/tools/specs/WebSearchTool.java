package com.hhoa.kline.core.core.tools.specs;

import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptContext;
import com.hhoa.kline.core.core.tools.ToolSpecProvider;
import com.hhoa.kline.core.core.tools.args.WebSearchInput;
import com.hhoa.kline.core.core.tools.handlers.WebSearchToolHandler;
import com.hhoa.kline.core.enums.ClineDefaultTool;
import java.util.function.Function;

/**
 * Web Search 工具规格 - 执行网络搜索并返回相关结果
 *
 * @author hhoa
 */
public final class WebSearchTool extends BaseToolSpec
        implements ToolSpecProvider<WebSearchInput, WebSearchToolHandler> {

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

    @Override
    public String id() {
        return ClineDefaultTool.WEB_SEARCH.getValue();
    }

    @Override
    public String description(ModelFamily family) {
        return switch (family) {
            case NATIVE_GPT_5, NATIVE_NEXT_GEN -> CONCISE_DESCRIPTION;
            default -> GENERIC_DESCRIPTION;
        };
    }

    @Override
    public Function<SystemPromptContext, Boolean> contextRequirements(ModelFamily family) {
        return context ->
                "cline".equals(context.getProviderId())
                        && Boolean.TRUE.equals(context.getClineWebToolsEnabled());
    }
}
