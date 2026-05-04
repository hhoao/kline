package com.hhoa.kline.core.core.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import com.hhoa.kline.core.core.tools.args.ExecuteCommandInput;
import com.hhoa.kline.core.core.tools.args.ListFilesArgs;
import com.hhoa.kline.core.core.tools.args.ReplaceInFileInput;
import com.hhoa.kline.core.core.tools.args.TodoWriteInput;
import com.hhoa.kline.core.core.tools.args.WriteToFileInput;
import com.hhoa.kline.core.core.tools.handlers.ToolHandler;
import com.hhoa.kline.core.core.tools.specs.WriteToFileTool;
import com.hhoa.kline.core.core.tools.specs.ExecuteCommandToolSpec;
import com.hhoa.kline.core.core.tools.specs.ListFilesTool;
import com.hhoa.kline.core.core.tools.types.ToolExecuteResult;
import java.lang.Record;
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
                        "Sample tool prompt.",
                        SampleArgs.class,
                        ModelFamily.GENERIC);

        assertEquals("sample_tool", spec.getName());
        assertEquals("Sample tool description.", spec.getDescription());
        assertEquals("Sample tool prompt.", spec.getPrompt());
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
        ExecuteCommandToolSpec provider = new ExecuteCommandToolSpec();
        ToolSpec spec = ToolSpecResolver.resolve(provider, ModelFamily.GENERIC);
        assertTrue(Modifier.isFinal(ExecuteCommandToolSpec.class.getModifiers()));
        assertEquals("execute_command", provider.name());
        assertEquals("execute_command", provider.name());
        assertEquals(ExecuteCommandInput.class, spec.getInputType());
        assertEquals(Record.class, spec.getInputType().getSuperclass());
        assertEquals("execute_command", spec.getName());
        assertEquals("boolean", parameter(spec, "requires_approval").get("type"));
        assertEquals("integer", parameter(spec, "timeout").get("type"));
        assertTrue(
                parameter(spec, "timeout").keySet().stream()
                        .noneMatch(key -> key.startsWith("x-kline-")));
    }

    @Test
    void executeCommandSpecUsesModelFamilySpecificDescriptions() {
        ExecuteCommandToolSpec provider = new ExecuteCommandToolSpec();

        assertEquals("Execute a CLI command on the user's system.", provider.description(ModelFamily.GENERIC));
        assertTrue(provider.prompt(ModelFamily.GEMINI_3).contains("When chaining commands"));
        assertTrue(provider.prompt(ModelFamily.GENERIC).contains("{{CWD}}{{MULTI_ROOT_HINT}}"));
        assertEquals(
                "Request to execute a CLI command on the system. Use this when you need to perform system operations or run specific commands to accomplish any step in the user's task.",
                provider.prompt(ModelFamily.NATIVE_GPT_5));
        assertEquals(
                "Request to execute a CLI command on the system. Use this when you need to perform system operations or run specific commands to accomplish any step in the user's task.",
                provider.prompt(ModelFamily.NATIVE_GPT_5_1));
        assertEquals(
                "Request to execute a CLI command on the system. Use this when you need to perform system operations or run specific commands to accomplish any step in the user's task.",
                provider.prompt(ModelFamily.NATIVE_NEXT_GEN));
    }

    @Test
    void listFilesHandlerDeclaresTypedArgumentsAndGeneratedSpec() {
        ToolSpec spec = ToolSpecResolver.resolve(new ListFilesTool(), ModelFamily.GENERIC);
        assertEquals(ListFilesArgs.class, spec.getInputType());
        assertEquals("list_files", spec.getName());
        assertEquals("boolean", parameter(spec, "recursive").get("type"));
        assertTrue(!properties(spec).containsKey("task_progress"));
    }

    @Test
    void defaultRegistryBindsFileSpecsToTypedInputs() {
        DefaultToolRegistry registry = new DefaultToolRegistry();

        ToolSpec writeSpec = registry.getToolSpec("write_to_file", ModelFamily.GENERIC);
        ToolSpec replaceSpec = registry.getToolSpec("replace_in_file", ModelFamily.GENERIC);

        assertNotNull(writeSpec);
        assertEquals(WriteToFileInput.class, writeSpec.getInputType());
        assertEquals(Record.class, writeSpec.getInputType().getSuperclass());
        assertEquals("string", parameter(writeSpec, "content").get("type"));
        assertTrue(!properties(writeSpec).containsKey("task_progress"));
        assertTrue(properties(writeSpec).containsKey("path"));
        assertTrue(properties(writeSpec).containsKey("absolutePath"));

        assertNotNull(replaceSpec);
        assertEquals(ReplaceInFileInput.class, replaceSpec.getInputType());
        assertEquals("string", parameter(replaceSpec, "diff").get("type"));
        assertTrue(!properties(replaceSpec).containsKey("task_progress"));
        assertTrue(properties(replaceSpec).containsKey("absolutePath"));
    }

    @Test
    void todoWriteUsesStructuredClaudeCodeTodoSchema() {
        DefaultToolRegistry registry = new DefaultToolRegistry();

        ToolSpec todoSpec = registry.getToolSpec("TodoWrite", ModelFamily.GENERIC);

        assertNotNull(todoSpec);
        assertEquals(TodoWriteInput.class, todoSpec.getInputType());
        assertEquals("TodoWrite", todoSpec.getName());
        assertTrue(properties(todoSpec).containsKey("todos"));
        assertTrue(todoSpec.getPrompt().contains("structured task list"));
    }

    @Test
    void nativeWriteSpecUsesSameInputRecordWithAbsolutePathInSchema() {
        DefaultToolRegistry registry = new DefaultToolRegistry();

        ToolSpec writeSpec = registry.getToolSpec("write_to_file", ModelFamily.NATIVE_GPT_5);

        assertNotNull(writeSpec);
        assertEquals(WriteToFileInput.class, writeSpec.getInputType());
        assertEquals("string", parameter(writeSpec, "absolutePath").get("type"));
        assertTrue(properties(writeSpec).containsKey("path"));
        assertEquals(
                WriteToFileInput.class,
                ToolSpecResolver.resolveArgumentType(
                        registry.getToolHandler("write_to_file").getClass()));
    }

    @Test
    void newRuleAliasUsesWriteFileHandlerDirectly() {
        DefaultToolRegistry registry = new DefaultToolRegistry();

        assertSame(WriteToFileTool.sharedFileHandler(), registry.getToolHandler("new_rule"));
        assertEquals(
                WriteToFileInput.class,
                ToolSpecResolver.resolveArgumentType(registry.getToolHandler("new_rule").getClass()));
    }

    @Test
    void defaultRegistryProvidesPromptToolSpecs() {
        DefaultToolRegistry registry = new DefaultToolRegistry();

        List<ToolSpec> specs = registry.getToolSpecs(ModelFamily.GENERIC, null, true);

        assertTrue(specs.stream().anyMatch(spec -> "execute_command".equals(spec.getName())));
        assertTrue(specs.stream().anyMatch(spec -> "read_file".equals(spec.getName())));
        assertTrue(specs.stream().anyMatch(spec -> "replace_in_file".equals(spec.getName())));
    }

    @Test
    void handlerGenericInputTypeBecomesInputSchema() {
        ToolSpec spec =
                ToolSpecResolver.resolve(
                        "generic_handler",
                        "generic_handler",
                        "generic",
                        "generic prompt",
                        ModelFamily.GENERIC,
                        new GenericInputHandler());

        assertEquals("generic_handler", spec.getName());
        assertEquals("generic prompt", spec.getPrompt());
        assertEquals(GenericArgs.class, spec.getInputType());
        assertEquals("string", parameter(spec, "command").get("type"));
        assertEquals("boolean", parameter(spec, "requires_approval").get("type"));
        assertEquals("integer", parameter(spec, "timeout").get("type"));
        assertEquals(3, properties(spec).size());
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

    record GenericArgs(
            String command,
            @JsonProperty("requires_approval") Boolean requiresApproval,
            Integer timeout) {}

    static class GenericInputHandler implements ToolHandler<GenericArgs> {
        @Override
        public String getDescription(com.hhoa.kline.core.core.assistant.ToolUse block) {
            return "generic";
        }

        @Override
        public void handlePartialBlock(
                GenericArgs input,
                com.hhoa.kline.core.core.tools.types.ToolContext context,
                com.hhoa.kline.core.core.assistant.ToolUse block) {}

        @Override
        public ToolExecuteResult execute(
                GenericArgs input,
                com.hhoa.kline.core.core.tools.types.ToolContext context,
                com.hhoa.kline.core.core.assistant.ToolUse block) {
            return new ToolExecuteResult.Immediate(List.of());
        }

        public ToolExecuteResult execute(
                @JsonProperty(value = "command", required = true) String command,
                com.hhoa.kline.core.core.tools.types.ToolContext context,
                com.hhoa.kline.core.core.assistant.ToolUse block,
                String ignored) {
            throw new AssertionError("schema must not be resolved from public overloads");
        }
    }
}
