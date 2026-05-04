package com.hhoa.kline.core.core.tools.specs;

import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import com.hhoa.kline.core.core.tools.ToolSpecProvider;
import com.hhoa.kline.core.core.tools.args.ListFilesArgs;
import com.hhoa.kline.core.core.tools.handlers.ListFilesToolHandler;
import com.hhoa.kline.core.core.tools.handlers.ToolHandler;
import com.hhoa.kline.core.core.tools.ClineDefaultTool;

/** 列出文件工具规格。 */
public final class ListFilesTool implements ToolSpecProvider<ListFilesArgs> {

    private static final ListFilesToolHandler HANDLER = new ListFilesToolHandler();

    private static final String DESCRIPTION = "List files and directories within a directory.";

    private static final String PROMPT =
            "Request to list files and directories within the specified directory. If recursive is true, it will list all files and directories recursively. If recursive is false or not provided, it will only list the top-level contents. Do not use this tool to confirm the existence of files you may have created, as the user will let you know if the files were created successfully or not.";

    @Override
    public String name() {
        return ClineDefaultTool.LIST_FILES.getValue();
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
    public Class<ListFilesArgs> inputType(ModelFamily family) {
        return ListFilesArgs.class;
    }

    @Override
    public ToolHandler<ListFilesArgs> handler(ModelFamily family) {
        return HANDLER;
    }
}
