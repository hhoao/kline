package com.hhoa.kline.core.core.tools.specs;

import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import com.hhoa.kline.core.core.tools.ToolSpecProvider;
import com.hhoa.kline.core.core.tools.args.WriteToFileInput;
import com.hhoa.kline.core.core.tools.handlers.ToolHandler;
import com.hhoa.kline.core.enums.ClineDefaultTool;
import java.util.Map;
import java.util.Set;

/**
 * 写入文件工具规格
 *
 * @author hhoa
 */
public final class WriteToFileTool extends BaseToolSpec
        implements ToolSpecProvider<WriteToFileInput, ToolHandler> {

    private static final String GENERIC_DESCRIPTION =
            "Request to write content to a file at the specified path. If the file exists, it will be overwritten with the provided content. If the file doesn't exist, it will be created. This tool will automatically create any directories needed to write the file.";

    private static final String NATIVE_DESCRIPTION =
            "[IMPORTANT: Always output the absolutePath first] Request to write content to a file at the specified path. If the file exists, it will be overwritten with the provided content. If the file doesn't exist, it will be created. This tool will automatically create any directories needed to write the file.";

    @Override
    public String id() {
        return ClineDefaultTool.FILE_NEW.getValue();
    }

    @Override
    public String description(ModelFamily family) {
        return switch (family) {
            case NATIVE_GPT_5, NATIVE_GPT_5_1, NATIVE_NEXT_GEN -> NATIVE_DESCRIPTION;
            default -> GENERIC_DESCRIPTION;
        };
    }

    @Override
    public void customizeInputSchema(ModelFamily family, Map<String, Object> inputSchema) {
        if (isNative(family)) {
            require(inputSchema, "absolutePath");
            require(inputSchema, "content");
            describe(inputSchema, "absolutePath", "The absolute path to the file to write to.");
            describe(
                    inputSchema,
                    "content",
                    "After providing the path so a file can be created, then use this to provide the content to write to the file.");
            return;
        }
        require(inputSchema, "path");
        require(inputSchema, "content");
        describe(
                inputSchema,
                "path",
                "The path of the file to write to (relative to the current working directory {{CWD}}){{MULTI_ROOT_HINT}}");
        usage(inputSchema, "path", "File path here");
        describe(
                inputSchema,
                "content",
                "The content to write to the file. ALWAYS provide the COMPLETE intended content of the file, without any truncation or omissions. You MUST include ALL parts of the file, even if they haven't been modified.");
        usage(inputSchema, "content", "Your file content here");
    }

    @Override
    public Set<String> excludedParameters(ModelFamily family) {
        return isNative(family) ? Set.of("path") : Set.of("absolutePath");
    }

    private static boolean isNative(ModelFamily family) {
        return family == ModelFamily.NATIVE_GPT_5
                || family == ModelFamily.NATIVE_GPT_5_1
                || family == ModelFamily.NATIVE_NEXT_GEN;
    }
}
