package com.hhoa.kline.core.core.prompts.systemprompt.tools;

import com.hhoa.kline.core.core.prompts.systemprompt.ClineToolSpec;
import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import java.util.List;

/**
 * CLI 子代理工具规格
 *
 * @author hhoa
 */
public class CliSubagentsTool extends BaseToolSpec {

    public static ClineToolSpec create(ModelFamily modelFamily) {
        return ClineToolSpec.builder()
                .variant(modelFamily)
                .id("cli_subagents")
                .name("cli_subagents")
                .description("Use CLI subagents for focused tasks")
                .parameters(
                        List.of(
                                createParameter(
                                        "command",
                                        true,
                                        "The CLI command to execute",
                                        "Command here"),
                                createTaskProgressParameter()))
                .build();
    }
}
