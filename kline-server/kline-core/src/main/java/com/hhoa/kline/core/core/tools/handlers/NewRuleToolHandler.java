package com.hhoa.kline.core.core.tools.handlers;

import com.hhoa.kline.core.core.assistant.ToolUse;
import com.hhoa.kline.core.core.prompts.ResponseFormatter;
import com.hhoa.kline.core.core.task.AskResult;
import com.hhoa.kline.core.core.tools.args.WriteToFileInput;
import com.hhoa.kline.core.core.tools.types.ToolContext;
import com.hhoa.kline.core.core.tools.types.ToolExecuteResult;
import com.hhoa.kline.core.core.tools.types.ToolState;
import java.nio.file.Path;

/** new_rule 专属处理器。 */
public class NewRuleToolHandler implements StateFullToolHandler<WriteToFileInput> {
    private final WriteToFileToolHandler delegate = new WriteToFileToolHandler();
    private final ResponseFormatter formatResponse = new ResponseFormatter();

    @Override
    public ToolState createToolState() {
        return delegate.createToolState();
    }

    @Override
    public String getDescription(ToolUse block) {
        return delegate.getDescription(block);
    }

    @Override
    public void handlePartialBlock(WriteToFileInput input, ToolContext context, ToolUse block) {
        if (!isRulePath(input.path(), input.absolutePath())) {
            return;
        }
        delegate.handlePartialBlock(input, context, block);
    }

    @Override
    public ToolExecuteResult execute(WriteToFileInput input, ToolContext context, ToolUse block) {
        if (!isRulePath(input.path(), input.absolutePath())) {
            return HandlerUtils.createToolExecuteResult(
                    formatResponse.toolError(
                            "new_rule path must be under top-level .clinerules directory."));
        }
        return delegate.execute(input, context, block);
    }

    @Override
    public ToolExecuteResult resume(
            ToolContext context, ToolUse block, ToolState state, AskResult askResult) {
        return delegate.resume(context, block, state, askResult);
    }

    private boolean isRulePath(String path, String absolutePath) {
        String target = absolutePath != null && !absolutePath.isBlank() ? absolutePath : path;
        if (target == null || target.isBlank()) {
            return false;
        }
        String normalized = target.replace('\\', '/');
        if (normalized.startsWith(".clinerules/")) {
            return true;
        }
        Path p = Path.of(normalized).normalize();
        for (Path segment : p) {
            if (".clinerules".equals(segment.toString())) {
                return true;
            }
        }
        return false;
    }
}
