package com.hhoa.kline.core.core.tools.handlers;

import com.hhoa.kline.core.core.assistant.ToolUse;
import com.hhoa.kline.core.core.tools.types.ToolContext;
import com.hhoa.kline.core.core.tools.types.ToolExecuteResult;

/** 完全托管的工具接口：提供名称、描述与规范；执行入口由 ToolHandlerInvocationSupport 反射解析。 */
public interface ToolHandler<T> {

    String getDescription(ToolUse block);

    default boolean isConcurrencySafe(ToolUse block, ToolContext context) {
        return false;
    }

    void handlePartialBlock(T input, ToolContext context, ToolUse block);

    ToolExecuteResult execute(T input, ToolContext context, ToolUse block);
}
