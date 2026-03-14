package com.hhoa.kline.plugins.jdbc.core;

import lombok.extern.slf4j.Slf4j;

/** Schema上下文持有器 用于在线程中保存当前请求的schema名称 */
@Slf4j
public class SchemaContextHolder {

    private static final ThreadLocal<String> CONTEXT_HOLDER = new ThreadLocal<>();

    /**
     * 设置当前线程的schema名称
     *
     * @param schemaName schema名称
     */
    public static void setSchema(String schemaName) {
        CONTEXT_HOLDER.set(schemaName);
    }

    /**
     * 获取当前线程的schema名称
     *
     * @return schema名称
     */
    public static String getSchema() {
        return CONTEXT_HOLDER.get();
    }

    /** 清除当前线程的schema名称 */
    public static void clear() {
        CONTEXT_HOLDER.remove();
    }
}
