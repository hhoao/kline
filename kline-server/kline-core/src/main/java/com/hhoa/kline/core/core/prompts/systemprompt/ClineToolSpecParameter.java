package com.hhoa.kline.core.core.prompts.systemprompt;

import java.util.List;
import java.util.function.Function;
import lombok.Data;

/**
 * Cline 工具规格参数
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

        public ClineToolSpecParameter build() {
            return parameter;
        }
    }
}
