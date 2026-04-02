package com.hhoa.kline.core.core.hooks;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;

/** 全局注册表，追踪活跃的 hook 进程，用于清理 */
@Slf4j
public final class HookProcessRegistry {

    private static final Set<Process> ACTIVE_PROCESSES = ConcurrentHashMap.newKeySet();

    private HookProcessRegistry() {}

    public static void register(Process process) {
        ACTIVE_PROCESSES.add(process);
    }

    public static void unregister(Process process) {
        ACTIVE_PROCESSES.remove(process);
    }

    /** 强制终止所有活跃的 hook 进程 */
    public static void killAll() {
        for (Process process : ACTIVE_PROCESSES) {
            try {
                process.destroyForcibly();
            } catch (Exception e) {
                log.debug("Error killing hook process", e);
            }
        }
        ACTIVE_PROCESSES.clear();
    }

    public static int getActiveCount() {
        return ACTIVE_PROCESSES.size();
    }
}
