package com.hhoa.kline.core.core.tools;

import com.hhoa.kline.core.core.assistant.ToolUse;
import com.hhoa.kline.core.core.task.AskResult;
import com.hhoa.kline.core.core.tools.handlers.ToolHandler;
import com.hhoa.kline.core.core.tools.types.PendingAskToken;
import com.hhoa.kline.core.core.tools.types.ToolContext;
import com.hhoa.kline.core.core.tools.types.ToolExecuteResult;
import com.hhoa.kline.core.core.tools.types.ToolState;

/** 工具执行协调器接口：按工具名获取处理器，并由处理器提供描述等能力。 */
public interface ToolExecutor {
    ToolRegistry getRegistry();

    default ToolHandler<?> getHandler(String toolName) {
        return getRegistry().getToolHandler(toolName);
    }

    ToolExecuteResult executeTool(ToolUse block, ToolContext config);

    ToolExecuteResult resume(PendingAskToken askToken, AskResult askResult, ToolContext context);

    ToolState getOrCreateToolState(String name);
}
