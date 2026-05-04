package com.hhoa.kline.core.core.tools.handlers;

import com.hhoa.kline.core.core.assistant.TextContentBlock;
import com.hhoa.kline.core.core.assistant.ToolUse;
import com.hhoa.kline.core.core.tools.args.TodoWriteInput;
import com.hhoa.kline.core.core.tools.types.ToolContext;
import com.hhoa.kline.core.core.tools.types.ToolExecuteResult;
import java.util.List;
import java.util.stream.Collectors;

public class TodoWriteToolHandler implements ToolHandler<TodoWriteInput> {

    @Override
    public String getDescription(ToolUse block) {
        return "[TodoWrite]";
    }

    @Override
    public void handlePartialBlock(TodoWriteInput input, ToolContext context, ToolUse block) {}

    @Override
    public ToolExecuteResult execute(TodoWriteInput input, ToolContext context, ToolUse block) {
        String markdown = allTodosCompleted(input) ? "" : toMarkdown(input);
        if (context.getCallbacks() != null) {
            context.getCallbacks().updateFCListFromToolResponse(markdown);
        }
        return new ToolExecuteResult.Immediate(
                List.of(
                        new TextContentBlock(
                                "Todos have been modified successfully. Continue to use the todo list to track progress.")));
    }

    private static String toMarkdown(TodoWriteInput input) {
        if (input == null || input.todos() == null || input.todos().isEmpty()) {
            return "";
        }
        return input.todos().stream()
                .map(TodoWriteToolHandler::toMarkdownItem)
                .collect(Collectors.joining("\n"));
    }

    private static String toMarkdownItem(TodoWriteInput.TodoItem item) {
        String checkbox =
                item.status() == TodoWriteInput.TodoStatus.COMPLETED ? "- [x] " : "- [ ] ";
        return checkbox + item.content();
    }

    private static boolean allTodosCompleted(TodoWriteInput input) {
        return input != null
                && input.todos() != null
                && !input.todos().isEmpty()
                && input.todos().stream()
                        .allMatch(item -> item.status() == TodoWriteInput.TodoStatus.COMPLETED);
    }
}
