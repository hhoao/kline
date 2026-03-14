package com.hhoa.kline.plugins.jdbc.tools;

import org.springframework.ai.chat.model.ToolContext;

public class ToolContextHolder {
    private static final ThreadLocal<ToolContext> contextHolder = new ThreadLocal<>();

    public static void setContext(ToolContext context) {
        contextHolder.set(context);
    }

    public static ToolContext getContext() {
        return contextHolder.get();
    }

    public static void clear() {
        contextHolder.remove();
    }
}
