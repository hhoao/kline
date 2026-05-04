package com.hhoa.kline.core.core.tools.handlers;

import com.hhoa.kline.core.core.assistant.ToolUse;
import com.hhoa.kline.core.core.tools.types.ToolContext;
import com.hhoa.kline.core.core.tools.types.ToolExecuteResult;

/** 完全托管的工具接口：Input 类型由泛型声明，执行入口由 ToolHandlerInvocationSupport 统一映射。 */
public interface ToolHandler<T> {

    String getDescription(ToolUse block);

    default boolean isConcurrencySafe(ToolUse block, ToolContext context) {
        return false;
    }

    void handlePartialBlock(T input, ToolContext context, ToolUse block);

    ToolExecuteResult execute(T input, ToolContext context, ToolUse block);
}
