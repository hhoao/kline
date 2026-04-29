package com.hhoa.kline.core.core.tools.specs;

import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import com.hhoa.kline.core.core.tools.ToolSpecProvider;
import com.hhoa.kline.core.core.tools.args.SearchFilesInput;
import com.hhoa.kline.core.core.tools.handlers.SearchFilesToolHandler;
import com.hhoa.kline.core.enums.ClineDefaultTool;

/**
 * 搜索文件工具规格
 *
 * @author hhoa
 */
public final class SearchFilesTool extends BaseToolSpec
        implements ToolSpecProvider<SearchFilesInput, SearchFilesToolHandler> {

    private static final String DESCRIPTION =
            "Request to perform a regex search across files in a specified directory, providing context-rich results. This tool searches for patterns or specific content across multiple files, displaying each match with encapsulating context.";

    @Override
    public String id() {
        return ClineDefaultTool.SEARCH.getValue();
    }

    @Override
    public String description(ModelFamily family) {
        return DESCRIPTION;
    }
}
