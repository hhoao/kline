package com.hhoa.kline.core.core.tools.specs;

import com.hhoa.kline.core.core.prompts.systemprompt.ClineToolSpec;
import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import com.hhoa.kline.core.enums.ClineDefaultTool;
import java.util.List;

/**
 * 写入文件工具规格
 *
 * @author hhoa
 */
public class WriteToFileTool extends BaseToolSpec {

    private static final String GENERIC_DESCRIPTION =
            "Request to write content to a file at the specified path. If the file exists, it will be overwritten with the provided content. If the file doesn't exist, it will be created. This tool will automatically create any directories needed to write the file.";

    private static final String NATIVE_DESCRIPTION =
            "[IMPORTANT: Always output the absolutePath first] Request to write content to a file at the specified path. If the file exists, it will be overwritten with the provided content. If the file doesn't exist, it will be created. This tool will automatically create any directories needed to write the file.";

    public static ClineToolSpec create(ModelFamily modelFamily) {
        if (modelFamily == ModelFamily.NATIVE_GPT_5
                || modelFamily == ModelFamily.NATIVE_GPT_5_1
                || modelFamily == ModelFamily.NATIVE_NEXT_GEN) {
            return createNativeVariant(modelFamily);
        }

        return createGenericVariant(modelFamily);
    }

    private static ClineToolSpec createGenericVariant(ModelFamily modelFamily) {
        return ClineToolSpec.builder()
                .variant(modelFamily)
                .id(ClineDefaultTool.FILE_NEW.getValue())
                .name(ClineDefaultTool.FILE_NEW.getValue())
                .description(GENERIC_DESCRIPTION)
                .parameters(
                        List.of(
                                createParameter(
                                        "path",
                                        true,
                                        "The path of the file to write to (relative to the current working directory {{CWD}}){{MULTI_ROOT_HINT}}",
                                        "File path here"),
                                createParameter(
                                        "content",
                                        true,
                                        "The content to write to the file. ALWAYS provide the COMPLETE intended content of the file, without any truncation or omissions. You MUST include ALL parts of the file, even if they haven't been modified.",
                                        "Your file content here"),
                                createTaskProgressParameter()))
                .build();
    }

    private static ClineToolSpec createNativeVariant(ModelFamily modelFamily) {
        return ClineToolSpec.builder()
                .variant(modelFamily)
                .id(ClineDefaultTool.FILE_NEW.getValue())
                .name(ClineDefaultTool.FILE_NEW.getValue())
                .description(NATIVE_DESCRIPTION)
                .parameters(
                        List.of(
                                createParameter(
                                        "absolutePath",
                                        true,
                                        "The absolute path to the file to write to.",
                                        null),
                                createParameter(
                                        "content",
                                        true,
                                        "After providing the path so a file can be created, then use this to provide the content to write to the file.",
                                        null),
                                createTaskProgressParameter()))
                .build();
    }
}
