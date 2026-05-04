package com.hhoa.kline.core.core.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.hhoa.kline.core.core.assistant.ToolUse;
import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import com.hhoa.kline.core.core.tools.handlers.ToolHandler;
import com.hhoa.kline.core.core.tools.types.ToolContext;
import com.hhoa.kline.core.core.tools.types.ToolExecuteResult;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ToolSpecProviderTest {

    @Test
    void providerDoesNotCreateToolSpecs() {
        assertFalse(
                Arrays.stream(ToolSpecProvider.class.getDeclaredMethods())
                        .anyMatch(method -> "create".equals(method.getName())));
        assertFalse(
                Arrays.stream(ToolSpecProvider.class.getDeclaredMethods())
                        .anyMatch(method -> "customizeInputSchema".equals(method.getName())));
        assertFalse(
                Arrays.stream(ToolSpecProvider.class.getDeclaredMethods())
                        .anyMatch(method -> "excludedParameters".equals(method.getName())));
        assertFalse(
                Arrays.stream(ToolSpecProvider.class.getDeclaredMethods())
                        .anyMatch(method -> "customizeInput".equals(method.getName())));
        assertFalse(
                Arrays.stream(ToolSpecProvider.class.getDeclaredMethods())
                        .anyMatch(method -> "parameterHints".equals(method.getName())));
        assertFalse(
                Arrays.stream(ToolSpecProvider.class.getDeclaredMethods())
                        .anyMatch(method -> "instruction".equals(method.getName())));
    }

    @Test
    void resolverCreatesToolSpecFromProviderMetadataAndHandlerInputSchema() {
        SampleToolSpecProvider provider = new SampleToolSpecProvider();
        ToolSpec spec = ToolSpecResolver.resolve(provider, ModelFamily.GENERIC);

        assertEquals("sample_tool", spec.getName());
        assertEquals("sample_tool", spec.getName());
        assertEquals("Generic description.", spec.getDescription());
        assertEquals("Use sample_tool when you need the sample prompt.", spec.getPrompt());
        assertEquals(SampleInput.class, spec.getInputType());

        Map<String, Object> path = parameter(spec, "path");
        assertEquals("string", path.get("type"));
        assertEquals("Path to inspect.", path.get("description"));
        assertEquals(true, required(spec).contains("path"));

        Map<String, Object> recursive = parameter(spec, "recursive");
        assertEquals("boolean", recursive.get("type"));
        assertEquals(false, required(spec).contains("recursive"));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> parameter(ToolSpec spec, String name) {
        return (Map<String, Object>)
                ((Map<String, Object>) spec.getInputSchema().get("properties")).get(name);
    }

    @SuppressWarnings("unchecked")
    private static List<String> required(ToolSpec spec) {
        return (List<String>) spec.getInputSchema().get("required");
    }

    static class SampleToolSpecProvider implements ToolSpecProvider<SampleInput> {

        private static final SampleHandler HANDLER = new SampleHandler();

        @Override
        public String name() {
            return "sample_tool";
        }

        @Override
        public String description(ModelFamily family) {
            return "Generic description.";
        }

        @Override
        public String prompt(ModelFamily family) {
            return "Use sample_tool when you need the sample prompt.";
        }

        @Override
        public Class<SampleInput> inputType(ModelFamily family) {
            return SampleInput.class;
        }

        @Override
        public ToolHandler<SampleInput> handler(ModelFamily family) {
            return HANDLER;
        }
    }

    record SampleInput(
            @JsonProperty(value = "path", required = true)
                    @JsonPropertyDescription("Path to inspect.")
                    String path,
            @JsonProperty("recursive") @JsonPropertyDescription("Whether to recurse.")
                    Boolean recursive) {}

    static class SampleHandler implements ToolHandler<SampleInput> {
        @Override
        public String getDescription(ToolUse block) {
            return "sample";
        }

        @Override
        public void handlePartialBlock(SampleInput input, ToolContext context, ToolUse block) {}

        @Override
        public ToolExecuteResult execute(SampleInput input, ToolContext context, ToolUse block) {
            return new ToolExecuteResult.Immediate(List.of());
        }
    }

    static class WrongInputHandler implements ToolHandler<WrongInput> {
        @Override
        public String getDescription(ToolUse block) {
            return "sample";
        }

        @Override
        public void handlePartialBlock(WrongInput input, ToolContext context, ToolUse block) {}

        @Override
        public ToolExecuteResult execute(WrongInput input, ToolContext context, ToolUse block) {
            return new ToolExecuteResult.Immediate(List.of());
        }
    }

    record WrongInput(String path) {}
}
