package com.hhoa.kline.core.core.tools;

import com.hhoa.kline.core.core.tools.handlers.ToolHandler;

public interface ToolRegistry {
    ToolHandler getHandler(String toolName);

    boolean has(String toolName);
}
