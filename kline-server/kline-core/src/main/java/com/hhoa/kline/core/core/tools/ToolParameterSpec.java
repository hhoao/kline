package com.hhoa.kline.core.core.tools;

import com.hhoa.ai.kline.commons.config.ConfigOption;
import com.hhoa.ai.kline.commons.config.ConfigOptions;
import com.hhoa.ai.kline.commons.config.description.Description;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptContext;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import lombok.Data;

/**
 * Tool input parameter contract.
 *
 * <p>{@link ConfigOption} carries the stable key, required flag, and human description while this
 * class keeps prompt-only usage text and provider JSON-schema hints.
 */
@Data
public class ToolParameterSpec {
    private ConfigOption<?> option;
    private String instruction;
    private String usage;
    private List<String> dependencies;
    private Function<SystemPromptContext, Boolean> contextRequirements;
    private String type;
    private Function<SystemPromptContext, String> instructionFn;
    private Object items;
    private Map<String, Object> properties;

    public String getName() {
        return option != null ? option.key() : null;
    }

    public boolean isRequired() {
        return option != null && option.isRequired();
    }

    public String getDescription() {
        return instruction;
    }

    public static ToolParameterSpecBuilder builder() {
        return new ToolParameterSpecBuilder();
    }

    public static class ToolParameterSpecBuilder {
        private final ToolParameterSpec parameter = new ToolParameterSpec();
        private String name;
        private boolean required;
        private String description;

        public ToolParameterSpecBuilder option(ConfigOption<?> option) {
            parameter.setOption(option);
            if (option != null) {
                this.name = option.key();
                this.required = option.isRequired();
            }
            return this;
        }

        public ToolParameterSpecBuilder name(String name) {
            this.name = name;
            return this;
        }

        public ToolParameterSpecBuilder required(Boolean required) {
            this.required = Boolean.TRUE.equals(required);
            return this;
        }

        public ToolParameterSpecBuilder instruction(String instruction) {
            parameter.setInstruction(instruction);
            this.description = instruction;
            return this;
        }

        public ToolParameterSpecBuilder usage(String usage) {
            parameter.setUsage(usage);
            return this;
        }

        public ToolParameterSpecBuilder dependencies(List<String> dependencies) {
            parameter.setDependencies(dependencies);
            return this;
        }

        public ToolParameterSpecBuilder description(String description) {
            this.description = description;
            return this;
        }

        public ToolParameterSpecBuilder contextRequirements(
                Function<SystemPromptContext, Boolean> contextRequirements) {
            parameter.setContextRequirements(contextRequirements);
            return this;
        }

        public ToolParameterSpecBuilder type(String type) {
            parameter.setType(type);
            return this;
        }

        public ToolParameterSpecBuilder instructionFn(
                Function<SystemPromptContext, String> instructionFn) {
            parameter.setInstructionFn(instructionFn);
            return this;
        }

        public ToolParameterSpecBuilder items(Object items) {
            parameter.setItems(items);
            return this;
        }

        public ToolParameterSpecBuilder properties(Map<String, Object> properties) {
            parameter.setProperties(properties);
            return this;
        }

        public ToolParameterSpec build() {
            if (parameter.getOption() == null) {
                parameter.setOption(buildOption());
            }
            if (parameter.getType() == null || parameter.getType().isBlank()) {
                parameter.setType("string");
            }
            return parameter;
        }

        private ConfigOption<?> buildOption() {
            String schemaType =
                    parameter.getType() == null || parameter.getType().isBlank()
                            ? "string"
                            : parameter.getType();
            ConfigOption<?> option =
                    switch (schemaType) {
                        case "boolean" -> ConfigOptions.key(name).booleanType().noDefaultValue();
                        case "integer" -> ConfigOptions.key(name).intType().noDefaultValue();
                        case "number" -> ConfigOptions.key(name).doubleType().noDefaultValue();
                        case "array" ->
                                ConfigOptions.key(name).stringType().asList().noDefaultValue();
                        case "object" -> ConfigOptions.key(name).mapType().noDefaultValue();
                        default -> ConfigOptions.key(name).stringType().noDefaultValue();
                    };
            option =
                    option.withDescription(
                            Description.builder()
                                    .text(description != null ? description : "")
                                    .build());
            return required ? option.required() : option;
        }
    }
}
