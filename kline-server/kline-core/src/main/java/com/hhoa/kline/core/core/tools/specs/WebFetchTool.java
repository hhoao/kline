package com.hhoa.kline.core.core.tools.specs;

import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptContext;
import com.hhoa.kline.core.core.tools.ToolSpecProvider;
import com.hhoa.kline.core.core.tools.args.WebFetchInput;
import com.hhoa.kline.core.core.tools.handlers.ToolHandler;
import com.hhoa.kline.core.core.tools.handlers.WebFetchToolHandler;
import com.hhoa.kline.core.core.tools.ClineDefaultTool;
import java.util.function.Function;

/**
 * Web 获取工具规格
 *
 * @author hhoa
 */
public final class WebFetchTool implements ToolSpecProvider<WebFetchInput> {

    private static final WebFetchToolHandler HANDLER = new WebFetchToolHandler();

    private static final String DESCRIPTION = "Fetch and analyze content from a URL.";

    private static final String GENERIC_PROMPT =
            "Fetches content from a specified URL and analyzes it using your prompt\n"
                    + "- Takes a URL and analysis prompt as input\n"
                    + "- Fetches the URL content and processes based on your prompt\n"
                    + "- Use this tool when you need to retrieve and analyze web content\n"
                    + "- IMPORTANT: If an MCP-provided web fetch tool is available, prefer using that tool instead of this one, as it may have fewer restrictions.\n"
                    + "- The URL must be a fully-formed valid URL\n"
                    + "- The prompt must be at least 2 characters\n"
                    + "- HTTP URLs will be automatically upgraded to HTTPS\n"
                    + "- This tool is read-only and does not modify any files";

    private static final String CONCISE_PROMPT =
            "Fetches and analyzes content from a specified URL. "
                    + "IMPORTANT: If an MCP-provided web fetch tool is available, prefer using that tool "
                    + "instead of this one, as it may have fewer restrictions.";

    @Override
    public String name() {
        return ClineDefaultTool.WEB_FETCH.getValue();
    }

    @Override
    public String description(ModelFamily family) {
        return DESCRIPTION;
    }

    @Override
    public String prompt(ModelFamily family) {
        return switch (family) {
            case NATIVE_GPT_5, NATIVE_NEXT_GEN -> CONCISE_PROMPT;
            default -> GENERIC_PROMPT;
        };
    }

    @Override
    public Function<SystemPromptContext, Boolean> contextRequirements(ModelFamily family) {
        return context ->
                "cline".equals(context.getProviderId())
                        && Boolean.TRUE.equals(context.getClineWebToolsEnabled());
    }

    @Override
    public Class<WebFetchInput> inputType(ModelFamily family) {
        return WebFetchInput.class;
    }

    @Override
    public ToolHandler<WebFetchInput> handler(ModelFamily family) {
        return HANDLER;
    }
}
