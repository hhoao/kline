package com.hhoa.kline.core.core.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import com.hhoa.kline.core.core.tools.args.ExecuteCommandInput;
import com.hhoa.kline.core.core.tools.args.ListFilesArgs;
import com.hhoa.kline.core.core.tools.args.ReplaceInFileInput;
import com.hhoa.kline.core.core.tools.args.WriteToFileInput;
import com.hhoa.kline.core.core.tools.handlers.ExecuteCommandToolHandler;
import com.hhoa.kline.core.core.tools.handlers.ListFilesToolHandler;
import com.hhoa.kline.core.core.tools.handlers.ToolHandler;
import com.hhoa.kline.core.core.tools.specs.ExecuteCommandToolSpec;
import com.hhoa.kline.core.core.tools.specs.ListFilesTool;
import com.hhoa.kline.core.core.tools.types.ToolContext;
import com.hhoa.kline.core.core.tools.types.ToolExecuteResult;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ToolSpecResolverTest {

    @Test
    void resolvesToolSpecFromInputType() {
        ToolSpec spec =
                ToolSpecResolver.resolve(
                        "sample_tool",
                        "sample_tool",
                        "Sample tool description.",
                        null,
                        SampleArgs.class,
                        ModelFamily.GENERIC);

        assertEquals("sample_tool", spec.getName());
        assertEquals("Sample tool description.", spec.getDescription());
        assertNotNull(spec.getInputSchema());

        Map<String, Object> path = parameter(spec, "path");
        assertTrue(required(spec).contains("path"));
        assertEquals("string", path.get("type"));
        assertEquals("Path to inspect.", path.get("description"));

        Map<String, Object> recursive = parameter(spec, "recursive");
        assertEquals("boolean", recursive.get("type"));
        assertEquals("Whether to recurse.", recursive.get("description"));
    }

    @Test
    void executeCommandHandlerDeclaresTypedArgumentsAndGeneratedSpec() {
        ExecuteCommandToolHandler handler = new ExecuteCommandToolHandler();

        ExecuteCommandToolSpec provider = new ExecuteCommandToolSpec();
        ToolSpec spec = ToolSpecResolver.resolve(provider, ModelFamily.GENERIC, handler);
        assertTrue(Modifier.isFinal(ExecuteCommandToolSpec.class.getModifiers()));
        assertEquals("execute_command", provider.id());
        assertEquals("execute_command", provider.name());
        assertEquals(ExecuteCommandInput.class, spec.getInputType());
        assertEquals("execute_command", spec.getName());
        assertEquals("boolean", parameter(spec, "requires_approval").get("type"));
        assertEquals("integer", parameter(spec, "timeout").get("type"));
    }

    @Test
    void executeCommandSpecUsesModelFamilySpecificDescriptions() {
        ExecuteCommandToolSpec provider = new ExecuteCommandToolSpec();

        assertEquals(
                "Request to execute a CLI command on the system. Use this when you need to perform system operations or run specific commands to accomplish any step in the user's task.",
                provider.description(ModelFamily.NATIVE_GPT_5));
        assertEquals(
                "Request to execute a CLI command on the system. Use this when you need to perform system operations or run specific commands to accomplish any step in the user's task.",
                provider.description(ModelFamily.NATIVE_GPT_5_1));
        assertEquals(
                "Request to execute a CLI command on the system. Use this when you need to perform system operations or run specific commands to accomplish any step in the user's task.",
                provider.description(ModelFamily.NATIVE_NEXT_GEN));
        assertTrue(provider.description(ModelFamily.GEMINI_3).contains("When chaining commands"));
        assertTrue(
                provider.description(ModelFamily.GENERIC).contains("{{CWD}}{{MULTI_ROOT_HINT}}"));
    }

    @Test
    void listFilesHandlerDeclaresTypedArgumentsAndGeneratedSpec() {
        ListFilesToolHandler handler = new ListFilesToolHandler();

        ToolSpec spec = ToolSpecResolver.resolve(new ListFilesTool(), ModelFamily.GENERIC, handler);
        assertEquals(ListFilesArgs.class, spec.getInputType());
        assertEquals("list_files", spec.getName());
        assertEquals("boolean", parameter(spec, "recursive").get("type"));
        assertEquals("string", parameter(spec, "task_progress").get("type"));
    }

    @Test
    void defaultRegistryBindsFileSpecsToTypedInputs() {
        DefaultToolRegistry registry = new DefaultToolRegistry();

        ToolSpec writeSpec = registry.getSpec("write_to_file", ModelFamily.GENERIC);
        ToolSpec replaceSpec = registry.getSpec("replace_in_file", ModelFamily.GENERIC);

        assertNotNull(writeSpec);
        assertEquals(WriteToFileInput.class, writeSpec.getInputType());
        assertEquals("string", parameter(writeSpec, "content").get("type"));

        assertNotNull(replaceSpec);
        assertEquals(ReplaceInFileInput.class, replaceSpec.getInputType());
        assertEquals("string", parameter(replaceSpec, "diff").get("type"));
    }

    @Test
    void defaultRegistryProvidesPromptToolSpecs() {
        DefaultToolRegistry registry = new DefaultToolRegistry();

        List<ToolSpec> specs = registry.getToolSpecs(ModelFamily.GENERIC, null);

        assertTrue(specs.stream().anyMatch(spec -> "execute_command".equals(spec.getId())));
        assertTrue(specs.stream().anyMatch(spec -> "read_file".equals(spec.getId())));
        assertTrue(specs.stream().anyMatch(spec -> "replace_in_file".equals(spec.getId())));
    }

    @Test
    void methodParametersBeforeContextBecomeInputSchema() {
        ToolSpec spec =
                ToolSpecResolver.resolve(
                        "multi_parameter",
                        "multi_parameter",
                        "multi",
                        null,
                        ModelFamily.GENERIC,
                        new MultiParameterHandler());

        assertEquals("multi_parameter", spec.getName());
        assertEquals("string", parameter(spec, "command").get("type"));
        assertEquals("boolean", parameter(spec, "requires_approval").get("type"));
        assertEquals(2, properties(spec).size());
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> parameter(ToolSpec spec, String name) {
        return (Map<String, Object>) properties(spec).get(name);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> properties(ToolSpec spec) {
        return (Map<String, Object>) spec.getInputSchema().get("properties");
    }

    @SuppressWarnings("unchecked")
    private static List<String> required(ToolSpec spec) {
        return (List<String>) spec.getInputSchema().get("required");
    }

    record SampleArgs(
            @JsonProperty(value = "path", required = true)
                    @JsonPropertyDescription("Path to inspect.")
                    String path,
            @JsonProperty(value = "recursive") @JsonPropertyDescription("Whether to recurse.")
                    Boolean recursive,
            List<String> tags) {}

    static class MultiParameterHandler implements ToolHandler {
        @Override
        public String getDescription(com.hhoa.kline.core.core.assistant.ToolUse block) {
            return "multi";
        }

        public ToolExecuteResult execute(
                @JsonProperty(value = "command", required = true) String command,
                @JsonProperty("requires_approval") Boolean requiresApproval,
                ToolContext context) {
            return new ToolExecuteResult.Immediate(List.of());
        }
    }
}
