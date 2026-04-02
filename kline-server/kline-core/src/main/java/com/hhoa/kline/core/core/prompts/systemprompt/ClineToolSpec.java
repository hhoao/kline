package com.hhoa.kline.core.core.prompts.systemprompt;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import lombok.Builder;
import lombok.Data;

/**
 * Cline 工具规格
 *
 * @author hhoa
 */
@Data
public class ClineToolSpec {

    private ModelFamily variant;

    private String id;

    private String name;

    private String description;

    private String instruction;

    private Function<SystemPromptContext, Boolean> contextRequirements;

    private List<ClineToolSpecParameter> parameters;

    public static ClineToolSpecBuilder builder() {
        return new ClineToolSpecBuilder();
    }

    public static class ClineToolSpecBuilder {
        private final ClineToolSpec spec = new ClineToolSpec();

        public ClineToolSpecBuilder variant(ModelFamily variant) {
            spec.setVariant(variant);
            return this;
        }

        public ClineToolSpecBuilder id(String id) {
            spec.setId(id);
            return this;
        }

        public ClineToolSpecBuilder name(String name) {
            spec.setName(name);
            return this;
        }

        public ClineToolSpecBuilder description(String description) {
            spec.setDescription(description);
            return this;
        }

        public ClineToolSpecBuilder instruction(String instruction) {
            spec.setInstruction(instruction);
            return this;
        }

        public ClineToolSpecBuilder contextRequirements(
                Function<SystemPromptContext, Boolean> contextRequirements) {
            spec.setContextRequirements(contextRequirements);
            return this;
        }

        public ClineToolSpecBuilder parameters(List<ClineToolSpecParameter> parameters) {
            spec.setParameters(parameters);
            return this;
        }

        public ClineToolSpec build() {
            return spec;
        }
    }

    @Builder
    @Data
    public static class ClineToolSpecParameter {
        private String name;
        private boolean required;
        private String instruction;
        private String usage;

        private List<String> dependencies;

        private String description;
        private Function<SystemPromptContext, Boolean> contextRequirements;

        /**
         * 参数类型，默认为 "string"。
         * 支持: "string", "boolean", "integer", "array", "object"
         */
        private String type;

        /**
         * 动态 instruction 函数，优先级高于静态 instruction。
         * 当不为 null 时，使用此函数根据上下文生成 instruction。
         */
        private Function<SystemPromptContext, String> instructionFn;

        /** array 类型的元素 schema */
        private Object items;

        /** object 类型的属性定义 */
        private Map<String, Object> properties;
    }
}
