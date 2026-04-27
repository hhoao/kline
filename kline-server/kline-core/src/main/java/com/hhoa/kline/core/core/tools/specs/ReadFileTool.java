package com.hhoa.kline.core.core.tools.specs;

import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import com.hhoa.kline.core.core.tools.ToolSpec;
import com.hhoa.kline.core.enums.ClineDefaultTool;
import java.util.List;

/**
 * 读取文件工具规格
 *
 * @author hhoa
 */
public class ReadFileTool extends BaseToolSpec {

    public static ToolSpec create(ModelFamily modelFamily) {
        return ToolSpec.builder()
                .variant(modelFamily)
                .id(ClineDefaultTool.FILE_READ.getValue())
                .name(ClineDefaultTool.FILE_READ.getValue())
                .description(
                        "Request to read the contents of a file at the specified path. Use this when you need to examine the contents of an existing file you do not know the contents of, for example to analyze code, review text files, or extract information from configuration files. Automatically extracts raw text from PDF and DOCX files. May not be suitable for other types of binary files, as it returns the raw content as a string. Do NOT use this tool to list the contents of a directory. Only use this tool on files. You can optionally specify start_line and end_line to read only a specific range of lines from the file (1-based line numbers). This is especially useful for large files where you only need to examine a portion of the content.")
                .parameters(
                        List.of(
                                createParameter(
                                        "path",
                                        true,
                                        "The path of the file to read (relative to the current working directory {{CWD}}){{MULTI_ROOT_HINT}}",
                                        "File path here"),
                                createParameter(
                                        "start_line",
                                        false,
                                        "The starting line number (1-based) to read from. If not specified, reads from the beginning of the file. Must be used together with end_line.",
                                        "Start line number here"),
                                createParameter(
                                        "end_line",
                                        false,
                                        "The ending line number (1-based) to read to. If not specified, reads to the end of the file. Must be used together with start_line.",
                                        "End line number here"),
                                createTaskProgressParameter()))
                .build();
    }
}
