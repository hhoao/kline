package com.hhoa.kline.core.core.prompts.systemprompt.tools;

import com.hhoa.kline.core.core.prompts.systemprompt.ClineToolSpec;
import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import com.hhoa.kline.core.enums.ClineDefaultTool;
import java.util.List;

/**
 * 搜索文件工具规格
 *
 * @author hhoa
 */
public class SearchFilesTool extends BaseToolSpec {

    public static ClineToolSpec create(ModelFamily modelFamily) {
        return ClineToolSpec.builder()
                .variant(modelFamily)
                .id(ClineDefaultTool.SEARCH.getValue())
                .name(ClineDefaultTool.SEARCH.getValue())
                .description(
                        "Request to perform a regex search across files in a specified directory, providing context-rich results. This tool searches for patterns or specific content across multiple files, displaying each match with encapsulating context.")
                .parameters(
                        List.of(
                                createParameter(
                                        "path",
                                        true,
                                        "The path of the directory to search in (relative to the current working directory {{CWD}}){{MULTI_ROOT_HINT}}. This directory will be recursively searched.",
                                        "Directory path here"),
                                createParameter(
                                        "regex",
                                        true,
                                        "The regular expression pattern to search for. Uses Rust regex syntax.",
                                        "Your regex pattern here"),
                                createParameter(
                                        "file_pattern",
                                        false,
                                        "Glob pattern to filter files (e.g., '*.ts' for TypeScript files). If not provided, it will search all files (*).",
                                        "file pattern here (optional)"),
                                createTaskProgressParameter()))
                .build();
    }
}
