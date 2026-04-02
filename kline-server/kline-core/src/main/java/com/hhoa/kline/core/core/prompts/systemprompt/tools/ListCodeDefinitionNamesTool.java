package com.hhoa.kline.core.core.prompts.systemprompt.tools;

import com.hhoa.kline.core.core.prompts.systemprompt.ClineToolSpec;
import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import com.hhoa.kline.core.enums.ClineDefaultTool;
import java.util.List;

/**
 * 列出代码定义名称工具规格
 *
 * @author hhoa
 */
public class ListCodeDefinitionNamesTool extends BaseToolSpec {

    public static ClineToolSpec create(ModelFamily modelFamily) {
        return ClineToolSpec.builder()
                .variant(modelFamily)
                .id(ClineDefaultTool.LIST_CODE_DEF.getValue())
                .name(ClineDefaultTool.LIST_CODE_DEF.getValue())
                .description(
                        "Request to list definition names (classes, functions, methods, etc.) used in source code files at the top level of the specified directory. This tool provides insights into the codebase structure and important constructs, encapsulating high-level concepts and relationships that are crucial for understanding the overall architecture.")
                .parameters(
                        List.of(
                                createParameter(
                                        "path",
                                        true,
                                        "The path of a directory (not a file) relative to the current working directory {{CWD}}{{MULTI_ROOT_HINT}}. Lists definitions across all source files in that directory. To inspect a single file, use read_file instead.",
                                        "Directory path here"),
                                createTaskProgressParameter()))
                .build();
    }
}
