package com.hhoa.kline.core.core.tools;

import com.hhoa.kline.core.core.assistant.ToolUse;
import com.hhoa.kline.core.core.tools.args.WriteToFileInput;
import com.hhoa.kline.core.core.tools.handlers.ToolHandler;
import com.hhoa.kline.core.core.tools.types.ToolContext;
import com.hhoa.kline.core.core.tools.types.ToolExecuteResult;
import com.hhoa.kline.core.enums.ClineDefaultTool;

/** 共享工具处理器包装器：允许单个工具处理器以多个名称注册。 用于共享相同实现逻辑的工具（如 write_to_file、replace_in_file、new_rule）。 */
public class SharedToolHandler implements ToolHandler<WriteToFileInput> {
    @SuppressWarnings("unused")
    private final ClineDefaultTool name;
    private final ToolHandler<?> baseHandler;

    public SharedToolHandler(ClineDefaultTool name, ToolHandler<?> baseHandler) {
        this.name = name;
        this.baseHandler = baseHandler;
    }

    @Override
    public String getDescription(ToolUse block) {
        return baseHandler.getDescription(block);
    }

    @Override
    public void handlePartialBlock(WriteToFileInput input, ToolContext context, ToolUse block) {
        ToolHandlerInvocationSupport.handlePartialBlock(baseHandler, context, block);
    }

    @Override
    public ToolExecuteResult execute(WriteToFileInput input, ToolContext context, ToolUse block) {
        return ToolHandlerInvocationSupport.invoke(baseHandler, context, block);
    }
}
