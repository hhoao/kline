package com.hhoa.kline.core.core.task;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** 任务文件夹加锁/解锁工具。 服务端简单实现：使用内存中的字段作为锁，防止同一任务并发访问。 */
public final class TaskLockUtils {

    private TaskLockUtils() {}

    private static final Map<String, String> TASK_LOCKS = new ConcurrentHashMap<>();

    private static final String TASKS_BASE_PATH =
            System.getProperty("user.home") + "/.cline/data/tasks";

    public record FolderLockWithRetryResult(
            boolean acquired, boolean skipped, String conflictingLock) {}

    public static FolderLockWithRetryResult tryAcquireTaskLockWithRetry(String taskId) {
        String lockTarget = TASKS_BASE_PATH + "/" + taskId;

        String existingHolder = TASK_LOCKS.computeIfAbsent(lockTarget, k -> taskId);

        if (existingHolder.equals(taskId)) {
            return new FolderLockWithRetryResult(true, false, null);
        } else {
            return new FolderLockWithRetryResult(false, false, existingHolder);
        }
    }

    public static void releaseTaskLock(String taskId) {
        String lockTarget = TASKS_BASE_PATH + "/" + taskId;
        String currentHolder = TASK_LOCKS.get(lockTarget);

        if (currentHolder != null && currentHolder.equals(taskId)) {
            TASK_LOCKS.remove(lockTarget);
        }
    }
}
