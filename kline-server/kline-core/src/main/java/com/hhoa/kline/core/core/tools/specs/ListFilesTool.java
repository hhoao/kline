package com.hhoa.kline.core.core.tools.specs;

import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import com.hhoa.kline.core.core.tools.ToolSpecProvider;
import com.hhoa.kline.core.core.tools.args.ListFilesArgs;
import com.hhoa.kline.core.core.tools.handlers.ListFilesToolHandler;
import com.hhoa.kline.core.enums.ClineDefaultTool;

/** 列出文件工具规格。 */
public final class ListFilesTool implements ToolSpecProvider<ListFilesArgs, ListFilesToolHandler> {

    private static final String DESCRIPTION =
            "Request to list files and directories within the specified directory. If recursive is true, it will list all files and directories recursively. If recursive is false or not provided, it will only list the top-level contents. Do not use this tool to confirm the existence of files you may have created, as the user will let you know if the files were created successfully or not.";

    @Override
    public String id() {
        return ClineDefaultTool.LIST_FILES.getValue();
    }

    @Override
    public String description(ModelFamily family) {
        return DESCRIPTION;
    }
}
