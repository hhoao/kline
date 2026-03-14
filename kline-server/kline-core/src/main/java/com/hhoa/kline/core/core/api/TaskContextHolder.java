package com.hhoa.kline.core.core.api;

public final class TaskContextHolder {

    private static final ThreadLocal<TaskContext> HOLDER = new InheritableThreadLocal<>();

    public static void set(TaskContext context) {
        HOLDER.set(context);
    }

    public static TaskContext get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }

    private TaskContextHolder() {}
}
