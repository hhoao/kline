package com.hhoa.kline.core.core.tools.handlers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.hhoa.kline.core.core.assistant.TextContentBlock;
import com.hhoa.kline.core.core.assistant.ToolUse;
import com.hhoa.kline.core.core.shared.ClineAsk;
import com.hhoa.kline.core.core.shared.ClineMessageFormat;
import com.hhoa.kline.core.core.shared.ClineSay;
import com.hhoa.kline.core.core.tools.args.TodoWriteInput;
import com.hhoa.kline.core.core.tools.types.ToolContext;
import com.hhoa.kline.core.core.tools.types.ToolExecuteResult;
import java.util.List;
import org.junit.jupiter.api.Test;

class TodoWriteToolHandlerTest {

    @Test
    void updatesFocusChainWithStructuredTodos() {
        TodoWriteToolHandler handler = new TodoWriteToolHandler();
        CapturingCallbacks callbacks = new CapturingCallbacks();
        ToolContext context = ToolContext.builder().callbacks(callbacks).build();
        TodoWriteInput input =
                new TodoWriteInput(
                        List.of(
                                new TodoWriteInput.TodoItem(
                                        "Inspect current task_progress usage",
                                        TodoWriteInput.TodoStatus.COMPLETED,
                                        "Inspecting current task_progress usage"),
                                new TodoWriteInput.TodoItem(
                                        "Add TodoWrite tool",
                                        TodoWriteInput.TodoStatus.IN_PROGRESS,
                                        "Adding TodoWrite tool")));

        ToolExecuteResult result = handler.execute(input, context, new ToolUse());

        assertEquals(
                "- [x] Inspect current task_progress usage\n- [ ] Add TodoWrite tool",
                callbacks.taskProgress);
        assertTrue(result instanceof ToolExecuteResult.Immediate);
        ToolExecuteResult.Immediate immediate = (ToolExecuteResult.Immediate) result;
        assertEquals(1, immediate.blocks().size());
        assertTrue(((TextContentBlock) immediate.blocks().getFirst()).getText().contains("Todos"));
    }

    @Test
    void clearsFocusChainWhenAllTodosAreCompleted() {
        TodoWriteToolHandler handler = new TodoWriteToolHandler();
        CapturingCallbacks callbacks = new CapturingCallbacks();
        ToolContext context = ToolContext.builder().callbacks(callbacks).build();
        TodoWriteInput input =
                new TodoWriteInput(
                        List.of(
                                new TodoWriteInput.TodoItem(
                                        "Inspect TodoWrite usage",
                                        TodoWriteInput.TodoStatus.COMPLETED,
                                        "Inspecting TodoWrite usage"),
                                new TodoWriteInput.TodoItem(
                                        "Add TodoWrite tool",
                                        TodoWriteInput.TodoStatus.COMPLETED,
                                        "Adding TodoWrite tool")));

        handler.execute(input, context, new ToolUse());

        assertEquals("", callbacks.taskProgress);
    }

    private static final class CapturingCallbacks implements ToolContext.Callbacks {
        private String taskProgress;

        @Override
        public void say(
                ClineSay type,
                String text,
                String[] images,
                String[] files,
                Boolean partial,
                ClineMessageFormat format) {}

        @Override
        public com.hhoa.kline.core.core.task.AskPending ask(
                ClineAsk type, String text, Boolean partial, ClineMessageFormat format) {
            return null;
        }

        @Override
        public void saveCheckpoint(Boolean isAttemptCompletionMessage, Long completionMessageTs) {}

        @Override
        public Boolean shouldAutoApproveToolWithPath(String toolName, String path) {
            return false;
        }

        @Override
        public Boolean shouldAutoApproveTool(String toolName) {
            return false;
        }

        @Override
        public String sayAndCreateMissingParamError(String toolName, String paramName) {
            return "";
        }

        @Override
        public ToolContext.ExecuteResult executeCommandTool(String command, Integer timeoutSeconds) {
            return null;
        }

        @Override
        public void sayUserFeedback(String text, String[] images, String[] files) {}

        @Override
        public Boolean switchToActMode() {
            return false;
        }

        @Override
        public Boolean updateFCListFromToolResponse(String text) {
            taskProgress = text;
            return true;
        }

        @Override
        public Boolean doesLatestTaskCompletionHaveNewChanges() {
            return false;
        }
    }
}
