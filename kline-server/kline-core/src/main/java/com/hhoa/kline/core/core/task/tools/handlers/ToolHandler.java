package com.hhoa.kline.core.core.task.tools.handlers;

import com.hhoa.kline.core.core.assistant.ToolUse;
import com.hhoa.kline.core.core.prompts.systemprompt.ClineToolSpec;
import com.hhoa.kline.core.core.task.tools.types.ToolContext;
import com.hhoa.kline.core.core.task.tools.types.ToolExecuteResult;
import com.hhoa.kline.core.core.task.tools.types.UIHelpers;

/** 完全托管的工具接口：提供名称、描述、部分块处理与执行。 */
public interface ToolHandler {
    String getName();

    String getDescription(ToolUse block);

    void handlePartialBlock(ToolUse block, UIHelpers uiHelpers);

    ToolExecuteResult execute(ToolContext context, ToolUse block);

    /** 返回该工具在 systemprompt/tools 中的规范，用于执行前做必选/可选参数校验；无规范可返回 null。 */
    ClineToolSpec getClineToolSpec();
}
