package com.hhoa.kline.core.core.tools.specs;

import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import com.hhoa.kline.core.core.tools.ToolSpecProvider;
import com.hhoa.kline.core.core.tools.args.WriteToFileInput;
import com.hhoa.kline.core.core.tools.handlers.ToolHandler;
import com.hhoa.kline.core.core.tools.handlers.WriteToFileToolHandler;
import com.hhoa.kline.core.core.tools.ClineDefaultTool;

/**
 * 写入文件工具规格
 *
 * @author hhoa
 */
public final class WriteToFileTool implements ToolSpecProvider<WriteToFileInput> {

    private static final WriteToFileToolHandler SHARED = new WriteToFileToolHandler();

    private static final String DESCRIPTION = "Write content to a file.";

    private static final String GENERIC_PROMPT =
            "Request to write content to a file at the specified path. If the file exists, it will be overwritten with the provided content. If the file doesn't exist, it will be created. This tool will automatically create any directories needed to write the file.";

    private static final String NATIVE_PROMPT =
            "[IMPORTANT: Always output the absolutePath first] Request to write content to a file at the specified path. If the file exists, it will be overwritten with the provided content. If the file doesn't exist, it will be created. This tool will automatically create any directories needed to write the file.";

    public static WriteToFileToolHandler sharedFileHandler() {
        return SHARED;
    }

    @Override
    public String name() {
        return ClineDefaultTool.FILE_NEW.getValue();
    }

    @Override
    public String description(ModelFamily family) {
        return DESCRIPTION;
    }

    @Override
    public String prompt(ModelFamily family) {
        return switch (family) {
            case NATIVE_GPT_5, NATIVE_GPT_5_1, NATIVE_NEXT_GEN -> NATIVE_PROMPT;
            default -> GENERIC_PROMPT;
        };
    }

    @Override
    public Class<WriteToFileInput> inputType(ModelFamily family) {
        return WriteToFileInput.class;
    }

    @Override
    public ToolHandler<WriteToFileInput> handler(ModelFamily family) {
        return SHARED;
    }
}
