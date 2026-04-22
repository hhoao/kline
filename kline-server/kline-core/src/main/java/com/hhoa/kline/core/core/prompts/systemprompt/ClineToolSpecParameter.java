package com.hhoa.kline.core.core.prompts.systemprompt;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import lombok.Data;

/**
 * Cline 工具规格参数（独立类，与 ClineToolSpec.ClineToolSpecParameter 内部类保持一致）
 *
 * @author hhoa
 */
@Data
public class ClineToolSpecParameter {

    private String name;

    private Boolean required;

    private String instruction;

    private String usage;

    private List<String> dependencies;

    private String description;

    private Function<SystemPromptContext, Boolean> contextRequirements;

    /** 参数类型，默认为 "string"。 支持: "string", "boolean", "integer", "array", "object" */
    private String type;

    /** 动态 instruction 函数，优先级高于静态 instruction。 当不为 null 时，使用此函数根据上下文生成 instruction。 */
    private Function<SystemPromptContext, String> instructionFn;

    /** array 类型的元素 schema */
    private Object items;

    /** object 类型的属性定义 */
    private Map<String, Object> properties;

    public static ClineToolSpecParameterBuilder builder() {
        return new ClineToolSpecParameterBuilder();
    }

    public static class ClineToolSpecParameterBuilder {
        private final ClineToolSpecParameter parameter = new ClineToolSpecParameter();

        public ClineToolSpecParameterBuilder name(String name) {
            parameter.setName(name);
            return this;
        }

        public ClineToolSpecParameterBuilder required(Boolean required) {
            parameter.setRequired(required);
            return this;
        }

        public ClineToolSpecParameterBuilder instruction(String instruction) {
            parameter.setInstruction(instruction);
            return this;
        }

        public ClineToolSpecParameterBuilder usage(String usage) {
            parameter.setUsage(usage);
            return this;
        }

        public ClineToolSpecParameterBuilder dependencies(List<String> dependencies) {
            parameter.setDependencies(dependencies);
            return this;
        }

        public ClineToolSpecParameterBuilder description(String description) {
            parameter.setDescription(description);
            return this;
        }

        public ClineToolSpecParameterBuilder contextRequirements(
                Function<SystemPromptContext, Boolean> contextRequirements) {
            parameter.setContextRequirements(contextRequirements);
            return this;
        }

        public ClineToolSpecParameterBuilder type(String type) {
            parameter.setType(type);
            return this;
        }

        public ClineToolSpecParameterBuilder instructionFn(
                Function<SystemPromptContext, String> instructionFn) {
            parameter.setInstructionFn(instructionFn);
            return this;
        }

        public ClineToolSpecParameterBuilder items(Object items) {
            parameter.setItems(items);
            return this;
        }

        public ClineToolSpecParameterBuilder properties(Map<String, Object> properties) {
            parameter.setProperties(properties);
            return this;
        }

        public ClineToolSpecParameter build() {
            return parameter;
        }
    }
}
