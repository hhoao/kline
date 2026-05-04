package com.hhoa.kline.core.core.tools;

import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptContext;
import java.util.Map;
import java.util.function.Function;
import lombok.Data;

/**
 * Kline tool contract used by prompt rendering, native provider schema conversion, and execution
 * validation.
 *
 * @author hhoa
 */
@Data
public class ToolSpec {

    private ModelFamily variant;

    private String name;

    private String description;

    private String prompt;

    private Function<SystemPromptContext, Boolean> enabled;

    private Map<String, Object> inputSchema;

    private Class<?> inputType;

    public static ToolSpecBuilder builder() {
        return new ToolSpecBuilder();
    }

    public static class ToolSpecBuilder {
        private final ToolSpec spec = new ToolSpec();

        public ToolSpecBuilder variant(ModelFamily variant) {
            spec.setVariant(variant);
            return this;
        }

        public ToolSpecBuilder name(String name) {
            spec.setName(name);
            return this;
        }

        public ToolSpecBuilder description(String description) {
            spec.setDescription(description);
            return this;
        }

        public ToolSpecBuilder prompt(String prompt) {
            spec.setPrompt(prompt);
            return this;
        }

        public ToolSpecBuilder contextRequirements(
                Function<SystemPromptContext, Boolean> contextRequirements) {
            spec.setEnabled(contextRequirements);
            return this;
        }

        public ToolSpecBuilder inputSchema(Map<String, Object> inputSchema) {
            spec.setInputSchema(inputSchema);
            return this;
        }

        public ToolSpecBuilder inputType(Class<?> inputType) {
            spec.setInputType(inputType);
            return this;
        }

        public ToolSpec build() {
            return spec;
        }
    }
}
