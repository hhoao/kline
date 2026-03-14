package com.hhoa.kline.core.core.prompts.systemprompt.tools;

import com.hhoa.kline.core.core.prompts.systemprompt.ClineToolSpec;
import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import com.hhoa.kline.core.enums.ClineDefaultTool;
import java.util.List;

/**
 * 列出文件工具规格
 *
 * @author hhoa
 */
public class ListFilesTool extends BaseToolSpec {

    public static ClineToolSpec create(ModelFamily modelFamily) {
        return ClineToolSpec.builder()
                .variant(modelFamily)
                .id(ClineDefaultTool.LIST_FILES.getValue())
                .name(ClineDefaultTool.LIST_FILES.getValue())
                .description(
                        "Request to list files and directories within the specified directory. If recursive is true, it will list all files and directories recursively. If recursive is false or not provided, it will only list the top-level contents. Do not use this tool to confirm the existence of files you may have created, as the user will let you know if the files were created successfully or not.")
                .parameters(
                        List.of(
                                createParameter(
                                        "path",
                                        true,
                                        "The path of the directory to list contents for (relative to the current working directory {{CWD}}){{MULTI_ROOT_HINT}}",
                                        "Directory path here"),
                                createParameter(
                                        "recursive",
                                        false,
                                        "Whether to list files recursively. Use true for recursive listing, false or omit for top-level only.",
                                        "true or false (optional)"),
                                createTaskProgressParameter()))
                .build();
    }
}
