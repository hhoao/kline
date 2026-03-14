package com.hhoa.kline.core.core.task.tools;

import com.hhoa.kline.core.core.assistant.ToolUse;

/** 工具执行协调器接口：按工具名获取处理器，并由处理器提供描述等能力。 */
public interface ToolExecutorCoordinator {
    ToolHandler getHandler(String toolName);

    interface ToolHandler {
        String getDescription(ToolUse block);
    }
}
