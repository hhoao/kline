package com.hhoa.kline.core.core.prompts.systemprompt.tools;

import com.hhoa.kline.core.core.prompts.systemprompt.ClineToolSpec;
import com.hhoa.kline.core.enums.ClineDefaultTool;
import java.util.List;

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
    protected static ClineToolSpec.ClineToolSpecParameter createParameter(
            String name, boolean required, String instruction, String usage) {
        return ClineToolSpec.ClineToolSpecParameter.builder()
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
    protected static ClineToolSpec.ClineToolSpecParameter createTaskProgressParameter() {
        return ClineToolSpec.ClineToolSpecParameter.builder()
                .name("task_progress")
                .required(false)
                .instruction(
                        "A checklist showing task progress after this tool use is completed. (See 'Updating Task Progress' section for more details)")
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
    protected static ClineToolSpec.ClineToolSpecParameter createParameterWithDependency(
            String name,
            boolean required,
            String instruction,
            String usage,
            String description,
            String dependencyToolId) {
        ClineToolSpec.ClineToolSpecParameter.ClineToolSpecParameterBuilder builder =
                ClineToolSpec.ClineToolSpecParameter.builder()
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
}
