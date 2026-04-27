package com.hhoa.kline.core.core.tools.specs;

import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptContext;
import com.hhoa.kline.core.core.tools.ToolParameterSpec;
import com.hhoa.kline.core.enums.ClineDefaultTool;
import java.util.List;
import java.util.function.Function;

/**
 * 基础工具规格类，提供共用的辅助方法
 *
 * @author hhoa
 */
public abstract class BaseToolSpec {

    /**
     * 创建参数
     *
     * @param name 参数名称
     * @param required 是否必需
     * @param instruction 指令
     * @param usage 用法
     * @return 参数规格
     */
    protected static ToolParameterSpec createParameter(
            String name, boolean required, String instruction, String usage) {
        return ToolParameterSpec.builder()
                .name(name)
                .required(required)
                .instruction(instruction)
                .usage(usage)
                .build();
    }

    /**
     * 创建任务进度参数
     *
     * @return 任务进度参数规格
     */
    protected static ToolParameterSpec createTaskProgressParameter() {
        return ToolParameterSpec.builder()
                .name("task_progress")
                .required(false)
                .instruction(
                        "A checklist showing task progress after this tool use is completed. The task_progress parameter must be included as a separate parameter inside of the parent tool call, it must be separate from other parameters such as content, arguments, etc. (See 'UPDATING TASK PROGRESS' section for more details)")
                .usage("Checklist here (optional)")
                .dependencies(List.of(ClineDefaultTool.TODO.getValue()))
                .build();
    }

    /**
     * 创建带依赖的参数
     *
     * @param name 参数名称
     * @param required 是否必需
     * @param instruction 指令
     * @param usage 用法
     * @param description 描述（可选）
     * @param dependencyToolId 依赖的工具 ID（如 "focus_chain"）
     * @return 参数规格
     */
    protected static ToolParameterSpec createParameterWithDependency(
            String name,
            boolean required,
            String instruction,
            String usage,
            String description,
            String dependencyToolId) {
        ToolParameterSpec.ToolParameterSpecBuilder builder =
                ToolParameterSpec.builder()
                        .name(name)
                        .required(required)
                        .instruction(instruction)
                        .usage(usage)
                        .dependencies(dependencyToolId != null ? List.of(dependencyToolId) : null);
        if (description != null) {
            builder.description(description);
        }
        return builder.build();
    }

    /**
     * 创建带类型的参数
     *
     * @param name 参数名称
     * @param required 是否必需
     * @param instruction 指令
     * @param usage 用法
     * @param type 参数类型 ("string", "boolean", "integer", "array", "object")
     * @return 参数规格
     */
    protected static ToolParameterSpec createParameterWithType(
            String name, boolean required, String instruction, String usage, String type) {
        return ToolParameterSpec.builder()
                .name(name)
                .required(required)
                .instruction(instruction)
                .usage(usage)
                .type(type)
                .build();
    }

    /**
     * 创建带动态 instruction 函数的参数
     *
     * @param name 参数名称
     * @param required 是否必需
     * @param instructionFn 动态指令函数
     * @param usage 用法
     * @return 参数规格
     */
    protected static ToolParameterSpec createParameterWithInstructionFn(
            String name,
            boolean required,
            Function<SystemPromptContext, String> instructionFn,
            String usage) {
        return ToolParameterSpec.builder()
                .name(name)
                .required(required)
                .instructionFn(instructionFn)
                .usage(usage)
                .build();
    }
}
