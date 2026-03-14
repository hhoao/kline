package com.hhoa.kline.core.core.task.tools.utils;

import com.hhoa.kline.core.core.assistant.ToolUse;
import com.hhoa.kline.core.core.task.tools.ToolExecutorCoordinator;

/** 工具展示与格式化的辅助方法。 */
public final class ToolDisplayUtils {

    private ToolDisplayUtils() {}

    public static String getToolDescription(ToolUse block, ToolExecutorCoordinator coordinator) {
        if (coordinator != null && block != null) {
            ToolExecutorCoordinator.ToolHandler handler = coordinator.getHandler(block.getName());
            if (handler != null) {
                return handler.getDescription(block);
            }
        }
        return block == null ? "[unknown]" : "[" + block.getName() + "]";
    }
}
