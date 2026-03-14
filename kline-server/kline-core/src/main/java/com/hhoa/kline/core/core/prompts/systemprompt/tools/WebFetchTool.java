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
public class WebFetchTool extends BaseToolSpec {

    public static ClineToolSpec create(ModelFamily modelFamily) {
        return ClineToolSpec.builder()
                .variant(modelFamily)
                .id(ClineDefaultTool.WEB_FETCH.getValue())
                .name(ClineDefaultTool.WEB_FETCH.getValue())
                .description(
                        "Fetches content from a specified URL and processes into markdown\n"
                                + "- Takes a URL as input\n"
                                + "- Fetches the URL content, converts HTML to markdown\n"
                                + "- Use this tool when you need to retrieve and analyze web content\n"
                                + "- IMPORTANT: If an MCP-provided web fetch tool is available, prefer using that tool instead of this one, as it may have fewer restrictions.\n"
                                + "- The URL must be a fully-formed valid URL\n"
                                + "- HTTP URLs will be automatically upgraded to HTTPS\n"
                                + "- This tool is read-only and does not modify any files")
                .parameters(
                        List.of(
                                createParameter(
                                        "url",
                                        true,
                                        "The URL to fetch content from",
                                        "https://example.com/docs"),
                                createTaskProgressParameter()))
                .build();
    }
}
