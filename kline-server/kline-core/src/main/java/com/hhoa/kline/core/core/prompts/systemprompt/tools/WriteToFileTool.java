package com.hhoa.kline.core.core.prompts.systemprompt.tools;

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

    public static ClineToolSpec create(ModelFamily modelFamily) {
        return ClineToolSpec.builder()
                .variant(modelFamily)
                .id(ClineDefaultTool.FILE_NEW.getValue())
                .name(ClineDefaultTool.FILE_NEW.getValue())
                .description(
                        "Request to write content to a file at the specified path. If the file exists, it will be overwritten with the provided content. If the file doesn't exist, it will be created. This tool will automatically create any directories needed to write the file.")
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
}
