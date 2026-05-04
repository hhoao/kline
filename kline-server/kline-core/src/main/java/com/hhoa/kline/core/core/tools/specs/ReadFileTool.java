package com.hhoa.kline.core.core.tools.specs;

import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import com.hhoa.kline.core.core.tools.ToolSpecProvider;
import com.hhoa.kline.core.core.tools.args.ReadFileInput;
import com.hhoa.kline.core.core.tools.handlers.ReadFileToolHandler;
import com.hhoa.kline.core.core.tools.handlers.ToolHandler;
import com.hhoa.kline.core.core.tools.ClineDefaultTool;

/**
 * 读取文件工具规格
 *
 * @author hhoa
 */
public final class ReadFileTool implements ToolSpecProvider<ReadFileInput> {

    private static final ReadFileToolHandler HANDLER = new ReadFileToolHandler();

    private static final String DESCRIPTION = "Read the contents of a file.";

    private static final String PROMPT =
            "Request to read the contents of a file at the specified path. Use this when you need to examine the contents of an existing file you do not know the contents of, for example to analyze code, review text files, or extract information from configuration files. Automatically extracts raw text from PDF and DOCX files. May not be suitable for other types of binary files, as it returns the raw content as a string. Do NOT use this tool to list the contents of a directory. Only use this tool on files. You can optionally specify start_line and end_line to read only a specific range of lines from the file (1-based line numbers). This is especially useful for large files where you only need to examine a portion of the content.";

    @Override
    public String name() {
        return ClineDefaultTool.FILE_READ.getValue();
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
    public Class<ReadFileInput> inputType(ModelFamily family) {
        return ReadFileInput.class;
    }

    @Override
    public ToolHandler<ReadFileInput> handler(ModelFamily family) {
        return HANDLER;
    }
}
