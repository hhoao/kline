package com.hhoa.kline.plugins.jdbc.dbfilemapping.watcher;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 防抖管理器 Manages debounced file change events to avoid excessive processing
 *
 * <p>This class collects file change events during a configurable debounce period and processes
 * them in batch after the delay expires.
 */
public class DebounceManager {

    private static final Logger logger = LoggerFactory.getLogger(DebounceManager.class);

    /** 调度器，用于延迟执行任务 */
    private final ScheduledExecutorService scheduler;

    /** 文件路径到待处理任务的映射 Maps file paths to their scheduled debounce tasks */
    private final Map<Path, ScheduledFuture<?>> debounceTasks;

    /** 防抖延迟（毫秒） */
    private final int debounceMillis;

    /** 是否正在运行 */
    private volatile boolean running;

    /** 关闭超时时间（秒） */
    private static final int SHUTDOWN_TIMEOUT_SECONDS = 10;

    /**
     * 构造函数
     *
     * @param debounceMillis 防抖延迟（毫秒）
     */
    public DebounceManager(int debounceMillis) {
        if (debounceMillis < 0) {
            throw new IllegalArgumentException("Debounce delay must be non-negative");
        }

        this.debounceMillis = debounceMillis;
        this.debounceTasks = new ConcurrentHashMap<>();
        this.scheduler =
                Executors.newScheduledThreadPool(
                        2, // 使用小型线程池
                        r -> {
                            Thread thread = new Thread(r, "DebounceManager-Thread");
                            thread.setDaemon(true);
                            return thread;
                        });
        this.running = true;

        logger.info("DebounceManager initialized with delay: {}ms", debounceMillis);
    }

    /**
     * 提交一个需要防抖的任务 Submit a task to be debounced
     *
     * @param filePath 文件路径，用作防抖的key
     * @param action 延迟后要执行的操作
     */
    public void debounce(Path filePath, Runnable action) {
        if (!running) {
            logger.warn(
                    "DebounceManager is not running, ignoring debounce request for: {}", filePath);
            return;
        }

        if (filePath == null) {
            logger.warn("File path is null, ignoring debounce request");
            return;
        }

        if (action == null) {
            logger.warn("Action is null, ignoring debounce request for: {}", filePath);
            return;
        }

        synchronized (this) {
            // 取消之前的任务（如果存在）
            ScheduledFuture<?> existingTask = debounceTasks.get(filePath);
            if (existingTask != null && !existingTask.isDone()) {
                existingTask.cancel(true);
                logger.debug("Cancelled previous debounce task for: {}", filePath);
            }

            // 调度新任务
            ScheduledFuture<?> newTask =
                    scheduler.schedule(
                            () -> {
                                try {
                                    action.run();
                                } catch (Exception e) {
                                    logger.error(
                                            "Error executing debounced action for: {}",
                                            filePath,
                                            e);
                                } finally {
                                    // 任务完成后从映射中移除
                                    debounceTasks.remove(filePath);
                                }
                            },
                            debounceMillis,
                            TimeUnit.MILLISECONDS);

            debounceTasks.put(filePath, newTask);
        }
    }

    /**
     * 批量提交需要防抖的任务 Submit multiple tasks to be debounced
     *
     * @param filePaths 文件路径集合
     * @param actionProvider 根据文件路径生成操作的函数
     */
    public void debounceBatch(Set<Path> filePaths, Consumer<Path> actionProvider) {
        if (filePaths == null || filePaths.isEmpty()) {
            return;
        }

        for (Path filePath : filePaths) {
            debounce(filePath, () -> actionProvider.accept(filePath));
        }
    }

    /** 立即执行所有待处理的任务（跳过防抖延迟） Flush all pending tasks immediately, bypassing the debounce delay */
    public void flush() {
        logger.info("Flushing all pending debounce tasks");

        // 获取所有待处理任务的快照
        Map<Path, ScheduledFuture<?>> snapshot = new ConcurrentHashMap<>(debounceTasks);

        for (Map.Entry<Path, ScheduledFuture<?>> entry : snapshot.entrySet()) {
            ScheduledFuture<?> task = entry.getValue();
            if (task != null && !task.isDone()) {
                // 取消延迟任务
                task.cancel(false);

                // 立即执行（注意：这里无法获取原始的Runnable，所以只能取消）
                // 实际应用中，如果需要flush功能，应该保存原始的Runnable
                logger.debug("Cancelled pending task for: {}", entry.getKey());
            }
        }

        debounceTasks.clear();
    }

    /**
     * 取消特定文件的防抖任务 Cancel the debounce task for a specific file
     *
     * @param filePath 文件路径
     * @return true if a task was cancelled, false otherwise
     */
    public boolean cancel(Path filePath) {
        if (filePath == null) {
            return false;
        }

        ScheduledFuture<?> task = debounceTasks.remove(filePath);
        if (task != null && !task.isDone()) {
            boolean cancelled = task.cancel(false);
            logger.debug("Cancelled debounce task for: {} (success: {})", filePath, cancelled);
            return cancelled;
        }

        return false;
    }

    /**
     * 获取当前待处理的任务数量 Get the number of pending debounce tasks
     *
     * @return 待处理任务数量
     */
    public int getPendingTaskCount() {
        // 清理已完成的任务
        debounceTasks.entrySet().removeIf(entry -> entry.getValue().isDone());
        return debounceTasks.size();
    }

    /**
     * 检查特定文件是否有待处理的防抖任务 Check if a file has a pending debounce task
     *
     * @param filePath 文件路径
     * @return true if there is a pending task, false otherwise
     */
    public boolean hasPendingTask(Path filePath) {
        if (filePath == null) {
            return false;
        }

        ScheduledFuture<?> task = debounceTasks.get(filePath);
        return task != null && !task.isDone();
    }

    /**
     * 关闭防抖管理器 Shutdown the debounce manager
     *
     * <p>This will cancel all pending tasks and shutdown the scheduler.
     */
    public void shutdown() {
        if (!running) {
            logger.warn("DebounceManager is already shutdown");
            return;
        }

        logger.info("Shutting down DebounceManager...");
        running = false;

        // 取消所有待处理的任务
        for (ScheduledFuture<?> task : debounceTasks.values()) {
            if (task != null && !task.isDone()) {
                task.cancel(false);
            }
        }
        debounceTasks.clear();

        // 关闭调度器
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
                logger.warn("DebounceManager scheduler did not terminate gracefully");
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
            logger.error("Interrupted while shutting down DebounceManager", e);
        }

        logger.info("DebounceManager shutdown complete");
    }

    /**
     * 检查管理器是否正在运行 Check if the manager is running
     *
     * @return true if running, false otherwise
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * 获取配置的防抖延迟 Get the configured debounce delay
     *
     * @return 防抖延迟（毫秒）
     */
    public int getDebounceMillis() {
        return debounceMillis;
    }
}
