package com.hhoa.kline.core.core.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.hhoa.kline.core.core.assistant.TextContentBlock;
import com.hhoa.kline.core.core.assistant.ToolUse;
import com.hhoa.kline.core.core.assistant.UserContentBlock;
import com.hhoa.kline.core.core.model.http.HttpApiChunk;
import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptContext;
import com.hhoa.kline.core.core.task.ApiChunk;
import com.hhoa.kline.core.core.task.ApiHandler;
import com.hhoa.kline.core.core.tools.args.AgentInput;
import com.hhoa.kline.core.core.tools.handlers.AgentToolHandler;
import com.hhoa.kline.core.core.tools.handlers.ToolHandler;
import com.hhoa.kline.core.core.tools.types.ToolContext;
import com.hhoa.kline.core.core.tools.types.ToolExecuteResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import reactor.core.publisher.Flux;

class AgentToolMigrationTest {

    @Test
    void registryExposesClaudeCodeAgentToolAndLegacyTaskAlias() {
        DefaultToolRegistry registry = new DefaultToolRegistry();

        ToolSpec agentSpec = registry.getToolSpec("Agent", null);

        assertNotNull(agentSpec);
        assertEquals("Agent", agentSpec.getName());
        assertEquals("Agent", agentSpec.getName());
        assertTrue(registry.has("Agent"));
        assertFalse(registry.has("use_subagents"));
    }

    @Test
    void handlerRunsAgentLoopUntilAttemptCompletion() {
        ScriptedApiHandler apiHandler =
                new ScriptedApiHandler(
                        List.of(
                                """
                                <read_file>
                                <path>README.md</path>
                                </read_file>
                                """,
                                """
                                <attempt_completion>
                                <result>Found it.</result>
                                </attempt_completion>
                                """));
        CapturingToolHandler readFileHandler = new CapturingToolHandler("README contents");
        ToolRegistry registry = new SingleToolRegistry(readFileHandler);
        ToolExecutor executor = new FakeToolExecutor(registry);
        ToolContext context =
                ToolContext.builder()
                        .api(
                                () ->
                                        new ToolContext.Model() {
                                            @Override
                                            public String getId() {
                                                return "test-model";
                                            }
                                        })
                        .apiHandler(apiHandler)
                        .coordinator(executor)
                        .build();

        AgentToolHandler handler = new AgentToolHandler();
        ToolExecuteResult result =
                handler.execute(
                        new AgentInput("Read docs", "Inspect README", null, null, false),
                        context,
                        new ToolUse("Agent", Map.of(), false));

        assertTrue(result instanceof ToolExecuteResult.Immediate);
        String text = blockText((ToolExecuteResult.Immediate) result);
        assertTrue(text.contains("Found it."));
        assertEquals("README.md", readFileHandler.lastInput.get("path"));
        assertTrue(apiHandler.request(1).contains("README contents"));
    }

    @Test
    void handlerLoadsProjectClaudeAgentsBySubagentType(@TempDir Path tempDir) throws Exception {
        Path agentsDir = tempDir.resolve(".claude").resolve("agents");
        Files.createDirectories(agentsDir);
        Files.writeString(
                agentsDir.resolve("reviewer.md"),
                """
                ---
                name: reviewer
                description: Reviews implementation risk
                tools: read_file
                model: haiku
                ---
                You are a reviewer. Only read files.
                """);
        ScriptedApiHandler apiHandler =
                new ScriptedApiHandler(
                        List.of(
                                """
                                <attempt_completion>
                                <result>Reviewed.</result>
                                </attempt_completion>
                                """));
        ToolContext context =
                ToolContext.builder()
                        .cwd(tempDir.toString())
                        .apiHandler(apiHandler)
                        .coordinator(new FakeToolExecutor(new SingleToolRegistry(new CapturingToolHandler(""))))
                        .build();

        ToolExecuteResult result =
                new AgentToolHandler()
                        .execute(
                                new AgentInput("Review code", "Review this", "reviewer", null, false),
                                context,
                                new ToolUse("Agent", Map.of(), false));

        assertTrue(blockText((ToolExecuteResult.Immediate) result).contains("Reviewed."));
        assertTrue(apiHandler.systemPrompt(0).contains("You are a reviewer."));
    }

    private static String blockText(ToolExecuteResult.Immediate result) {
        StringBuilder out = new StringBuilder();
        for (UserContentBlock block : result.blocks()) {
            if (block instanceof TextContentBlock textContentBlock) {
                out.append(textContentBlock.getText());
            }
        }
        return out.toString();
    }

    private static final class ScriptedApiHandler implements ApiHandler {
        private final List<String> responses;
        private final List<String> requests = new ArrayList<>();
        private final List<String> systemPrompts = new ArrayList<>();

        private ScriptedApiHandler(List<String> responses) {
            this.responses = responses;
        }

        @Override
        public String getLastRequestId() {
            return null;
        }

        @Override
        public Flux<ApiChunk> createMessageStream(
                String systemPrompt,
                List<com.hhoa.kline.core.core.assistant.MessageParam> conversationHistory) {
            systemPrompts.add(systemPrompt);
            requests.add(conversationHistory.toString());
            String response = responses.get(requests.size() - 1);
            return Flux.just(HttpApiChunk.text(response), HttpApiChunk.usage(10, 5));
        }

        @Override
        public String getModelId() {
            return "test-model";
        }

        @Override
        public String getProviderId() {
            return "test-provider";
        }

        private String request(int index) {
            return requests.get(index);
        }

        private String systemPrompt(int index) {
            return systemPrompts.get(index);
        }
    }

    private static final class CapturingToolHandler implements ToolHandler<Map<String, Object>> {
        private final String result;
        private Map<String, Object> lastInput;

        private CapturingToolHandler(String result) {
            this.result = result;
        }

        @Override
        public String getDescription(ToolUse block) {
            return "[read_file]";
        }

        @Override
        public void handlePartialBlock(
                Map<String, Object> input, ToolContext context, ToolUse block) {}

        @Override
        public ToolExecuteResult execute(
                Map<String, Object> input, ToolContext context, ToolUse block) {
            lastInput = input;
            return new ToolExecuteResult.Immediate(List.of(new TextContentBlock(result)));
        }
    }

    private static final class SingleToolRegistry implements ToolRegistry {
        private final ToolHandler<?> handler;

        private SingleToolRegistry(ToolHandler<?> handler) {
            this.handler = handler;
        }

        @Override
        public ToolHandler getToolHandler(String toolName) {
            return ClineDefaultTool.FILE_READ.getValue().equals(toolName) ? handler : null;
        }

        @Override
        public ToolSpec getToolSpec(String toolName, ModelFamily family) {
            return ToolSpec.builder().name(toolName).name(toolName).inputSchema(new HashMap<>()).build();
        }

        @Override
        public boolean has(String toolName) {
            return ClineDefaultTool.FILE_READ.getValue().equals(toolName);
        }

        @Override
        public List<ToolSpec> getToolSpecs(ModelFamily variant, SystemPromptContext context, Boolean enabled) {
            return List.of();
        }

        @Override
        public List<ToolSpec> getToolSpecs(ModelFamily variant, SystemPromptContext context, List<String> names, Boolean enabled) {
            return List.of();
        }

        @Override
        public <T> DefaultToolRegistry register(ToolSpecProvider<T> specProvider) {
            return null;
        }
    }

    private static final class FakeToolExecutor implements ToolExecutor {
        private final ToolRegistry registry;

        private FakeToolExecutor(ToolRegistry registry) {
            this.registry = registry;
        }

        @Override
        public ToolRegistry getRegistry() {
            return registry;
        }

        @Override
        public ToolExecuteResult executeTool(ToolUse block, ToolContext config) {
            return ToolHandlerInvocationSupport.invoke(registry.getToolHandler(block.getName()), config, block);
        }

        @Override
        public ToolExecuteResult resume(
                com.hhoa.kline.core.core.tools.types.PendingAskToken askToken,
                com.hhoa.kline.core.core.task.AskResult askResult,
                ToolContext context) {
            throw new UnsupportedOperationException();
        }

        @Override
        public com.hhoa.kline.core.core.tools.types.ToolState getOrCreateToolState(String name) {
            return new com.hhoa.kline.core.core.tools.types.ToolState();
        }
    }
}
