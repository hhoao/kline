package com.hhoa.kline.core.core.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hhoa.kline.core.core.assistant.ToolUse;
import com.hhoa.kline.core.core.tools.handlers.ToolHandler;
import com.hhoa.kline.core.core.tools.types.ToolContext;
import com.hhoa.kline.core.core.tools.types.ToolExecuteResult;
import com.hhoa.kline.core.core.tools.types.UIHelpers;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ToolHandlerInvocationSupportTest {

    @Test
    void toolHandlerInterfaceDoesNotExposeRawExecutionMethods() {
        assertFalse(
                Stream.of(ToolHandler.class.getDeclaredMethods())
                        .anyMatch(method -> "getName".equals(method.getName())));
        assertFalse(
                Stream.of(ToolHandler.class.getDeclaredMethods())
                        .anyMatch(method -> "getToolSpec".equals(method.getName())));
        assertFalse(
                Stream.of(ToolHandler.class.getDeclaredMethods())
                        .anyMatch(
                                method ->
                                        "execute".equals(method.getName())
                                                && method.getParameterCount() == 2
                                                && method.getParameterTypes()[0].equals(
                                                        ToolContext.class)
                                                && method.getParameterTypes()[1].equals(
                                                        ToolUse.class)));
        assertFalse(
                Stream.of(ToolHandler.class.getDeclaredMethods())
                        .anyMatch(
                                method ->
                                        "handlePartialBlock".equals(method.getName())
                                                && method.getParameterCount() == 2
                                                && method.getParameterTypes()[0].equals(
                                                        ToolUse.class)
                                                && method.getParameterTypes()[1].equals(
                                                        UIHelpers.class)));
    }

    @Test
    void invokesTypedHandlerWithMappedArguments() {
        CapturingHandler handler = new CapturingHandler();
        ToolContext context = ToolContext.builder().build();
        Map<String, Object> params = new HashMap<>();
        params.put("command", "pwd");
        params.put("requires_approval", Boolean.FALSE);
        params.put("timeout", 30);
        ToolUse toolUse = new ToolUse("execute_command", params, false);

        ToolHandlerInvocationSupport.invoke(handler, context, toolUse);

        assertEquals("pwd", handler.arguments.command());
        assertEquals(Boolean.FALSE, handler.arguments.requiresApproval());
        assertEquals(30, handler.arguments.timeout());
        assertEquals(context, handler.context);
    }

    @Test
    void invokesTypedPartialHandlerWithMappedArguments() {
        CapturingHandler handler = new CapturingHandler();
        ToolContext context = ToolContext.builder().build();
        Map<String, Object> params = new HashMap<>();
        params.put("command", "npm test");
        ToolUse toolUse = new ToolUse("execute_command", params, true);

        ToolHandlerInvocationSupport.handlePartialBlock(handler, context, toolUse);

        assertEquals("npm test", handler.partialArguments.command());
        assertEquals(context, handler.partialContext);
    }

    @Test
    void delegatingHandlerMapsDynamicPromptToSubagentPromptOne() {
        PromptCapturingHandler delegate = new PromptCapturingHandler();
        DelegatingToolHandler handler =
                new DelegatingToolHandler(
                        "use_subagent_writer", delegate, ToolSpec.builder().build());
        Map<String, Object> params = new HashMap<>();
        params.put("prompt", "write tests");
        ToolUse toolUse = new ToolUse("use_subagent_writer", params, false);

        ToolHandlerInvocationSupport.invoke(handler, ToolContext.builder().build(), toolUse);

        assertEquals("write tests", delegate.input.prompt1());
    }

    static class CapturingHandler implements ToolHandler {
        private CommandArgs arguments;
        private ToolContext context;
        private CommandArgs partialArguments;
        private ToolContext partialContext;

        @Override
        public String getDescription(ToolUse block) {
            return "execute";
        }

        public ToolExecuteResult execute(CommandArgs input, ToolContext context) {
            this.arguments = input;
            this.context = context;
            return new ToolExecuteResult.Immediate(java.util.List.of());
        }

        public void handlePartialBlock(CommandArgs input, ToolContext context) {
            this.partialArguments = input;
            this.partialContext = context;
        }
    }

    record CommandArgs(
            String command,
            @JsonProperty("requires_approval") Boolean requiresApproval,
            Integer timeout) {}

    static class PromptCapturingHandler implements ToolHandler {
        private PromptInput input;

        @Override
        public String getDescription(ToolUse block) {
            return "subagent";
        }

        public ToolExecuteResult execute(PromptInput input, ToolContext context) {
            this.input = input;
            return new ToolExecuteResult.Immediate(java.util.List.of());
        }
    }

    record PromptInput(@JsonProperty("prompt_1") String prompt1) {}
}
