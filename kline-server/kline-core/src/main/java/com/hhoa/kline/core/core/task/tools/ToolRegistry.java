package com.hhoa.kline.core.core.task.tools;

import com.hhoa.kline.core.core.task.tools.handlers.ToolHandler;

public interface ToolRegistry {
    ToolHandler getHandler(String toolName);

    boolean has(String toolName);
}
