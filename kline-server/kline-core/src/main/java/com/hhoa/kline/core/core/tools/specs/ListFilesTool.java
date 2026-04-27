package com.hhoa.kline.core.core.tools.specs;

import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import com.hhoa.kline.core.core.tools.ToolSpec;
import com.hhoa.kline.core.enums.ClineDefaultTool;
import java.util.List;

/**
 * 列出文件工具规格
 *
 * @author hhoa
 */
public class ListFilesTool extends BaseToolSpec {

    private static final String DESCRIPTION =
            "Request to list files and directories within the specified directory. If recursive is true, it will list all files and directories recursively. If recursive is false or not provided, it will only list the top-level contents. Do not use this tool to confirm the existence of files you may have created, as the user will let you know if the files were created successfully or not.";

    public static ToolSpec create(ModelFamily modelFamily) {
        if (modelFamily == ModelFamily.NATIVE_GPT_5
                || modelFamily == ModelFamily.NATIVE_GPT_5_1
                || modelFamily == ModelFamily.NATIVE_NEXT_GEN) {
            return createNativeVariant(modelFamily);
        }

        return createGenericVariant(modelFamily);
    }

    private static ToolSpec createGenericVariant(ModelFamily modelFamily) {
        return ToolSpec.builder()
                .variant(modelFamily)
                .id(ClineDefaultTool.LIST_FILES.getValue())
                .name(ClineDefaultTool.LIST_FILES.getValue())
                .description(DESCRIPTION)
                .parameters(
                        List.of(
                                createParameter(
                                        "path",
                                        true,
                                        "The path of the directory to list contents for (relative to the current working directory {{CWD}}){{MULTI_ROOT_HINT}}",
                                        "Directory path here"),
                                createParameterWithType(
                                        "recursive",
                                        false,
                                        "Whether to list files recursively. Use true for recursive listing, false or omit for top-level only.",
                                        "true or false (optional)",
                                        "boolean"),
                                createTaskProgressParameter()))
                .build();
    }

    private static ToolSpec createNativeVariant(ModelFamily modelFamily) {
        return ToolSpec.builder()
                .variant(modelFamily)
                .id(ClineDefaultTool.LIST_FILES.getValue())
                .name(ClineDefaultTool.LIST_FILES.getValue())
                .description(DESCRIPTION)
                .parameters(
                        List.of(
                                createParameter(
                                        "path",
                                        true,
                                        "The path of the directory to list contents for.",
                                        null),
                                createParameterWithType(
                                        "recursive",
                                        false,
                                        "Whether to list files recursively. Use true for recursive listing, false or omit for top-level only.",
                                        null,
                                        "boolean"),
                                createTaskProgressParameter()))
                .build();
    }
}
