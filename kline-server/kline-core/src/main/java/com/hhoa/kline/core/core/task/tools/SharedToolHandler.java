package com.hhoa.kline.core.core.task.tools;

import com.hhoa.kline.core.core.assistant.ToolUse;
import com.hhoa.kline.core.core.prompts.systemprompt.ClineToolSpec;
import com.hhoa.kline.core.core.task.tools.handlers.ToolHandler;
import com.hhoa.kline.core.core.task.tools.types.ToolContext;
import com.hhoa.kline.core.core.task.tools.types.ToolExecuteResult;
import com.hhoa.kline.core.core.task.tools.types.UIHelpers;
import com.hhoa.kline.core.enums.ClineDefaultTool;

/** 共享工具处理器包装器：允许单个工具处理器以多个名称注册。 用于共享相同实现逻辑的工具（如 write_to_file、replace_in_file、new_rule）。 */
public class SharedToolHandler implements ToolHandler {
    private final ClineDefaultTool name;
    private final ToolHandler baseHandler;

    public SharedToolHandler(ClineDefaultTool name, ToolHandler baseHandler) {
        this.name = name;
        this.baseHandler = baseHandler;
    }

    @Override
    public String getName() {
        return name.getValue();
    }

    @Override
    public String getDescription(ToolUse block) {
        return baseHandler.getDescription(block);
    }

    @Override
    public void handlePartialBlock(ToolUse block, UIHelpers uiHelpers) {
        baseHandler.handlePartialBlock(block, uiHelpers);
    }

    @Override
    public ToolExecuteResult execute(ToolContext context, ToolUse block) {
        return baseHandler.execute(context, block);
    }

    @Override
    public ClineToolSpec getClineToolSpec() {
        return null;
    }
}
