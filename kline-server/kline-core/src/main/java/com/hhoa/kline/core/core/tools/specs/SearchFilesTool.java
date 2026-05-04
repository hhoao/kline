package com.hhoa.kline.core.core.tools.specs;

import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import com.hhoa.kline.core.core.tools.ToolSpecProvider;
import com.hhoa.kline.core.core.tools.args.SearchFilesInput;
import com.hhoa.kline.core.core.tools.handlers.SearchFilesToolHandler;
import com.hhoa.kline.core.core.tools.handlers.ToolHandler;
import com.hhoa.kline.core.core.tools.ClineDefaultTool;

/**
 * 搜索文件工具规格
 *
 * @author hhoa
 */
public final class SearchFilesTool implements ToolSpecProvider<SearchFilesInput> {

    private static final SearchFilesToolHandler HANDLER = new SearchFilesToolHandler();

    private static final String DESCRIPTION = "Search files in a directory with a regular expression.";

    private static final String PROMPT =
            "Request to perform a regex search across files in a specified directory, providing context-rich results. This tool searches for patterns or specific content across multiple files, displaying each match with encapsulating context.";

    @Override
    public String name() {
        return ClineDefaultTool.SEARCH.getValue();
    }

    @Override
    public String description(ModelFamily family) {
        return DESCRIPTION;
    }

    @Override
    public String prompt(ModelFamily family) {
        return PROMPT;
    }

    @Override
    public Class<SearchFilesInput> inputType(ModelFamily family) {
        return SearchFilesInput.class;
    }

    @Override
    public ToolHandler<SearchFilesInput> handler(ModelFamily family) {
        return HANDLER;
    }
}
