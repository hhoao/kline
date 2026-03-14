package com.hhoa.kline.plugins.jdbc.dbfilemapping.manager;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 资源管理工具类 Utility class for managing resource cleanup with graceful shutdown
 *
 * <p>Requirements: 10.4
 */
public class ResourceManager {

    private static final Logger logger = LoggerFactory.getLogger(ResourceManager.class);

    /**
     * 优雅关闭ExecutorService Gracefully shutdown an ExecutorService with timeout
     *
     * @param executor ExecutorService to shutdown
     * @param name Name of the executor for logging
     * @param timeoutSeconds Timeout in seconds
     * @return true if shutdown completed within timeout, false otherwise
     */
    public static boolean shutdownExecutor(
            ExecutorService executor, String name, int timeoutSeconds) {
        if (executor == null) {
            logger.debug("Executor {} is null, nothing to shutdown", name);
            return true;
        }

        if (executor.isShutdown()) {
            logger.debug("Executor {} is already shutdown", name);
            return true;
        }

        logger.debug("Shutting down executor: {}", name);

        try {
            // 发起优雅关闭
            // Initiate graceful shutdown
            executor.shutdown();

            // 等待任务完成
            // Wait for tasks to complete
            boolean terminated = executor.awaitTermination(timeoutSeconds, TimeUnit.SECONDS);

            if (!terminated) {
                // 超时后强制关闭
                // Force shutdown after timeout
                logger.warn(
                        "Executor {} did not terminate within {} seconds, forcing shutdown",
                        name,
                        timeoutSeconds);
                List<Runnable> pendingTasks = executor.shutdownNow();

                if (!pendingTasks.isEmpty()) {
                    logger.warn(
                            "Executor {} had {} pending tasks that were cancelled",
                            name,
                            pendingTasks.size());
                }

                // 再等待一小段时间确认强制关闭
                // Wait a bit more to confirm forced shutdown
                if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                    logger.error("Executor {} did not terminate even after forced shutdown", name);
                    return false;
                } else {
                    logger.info("Executor {} terminated after forced shutdown", name);
                    return true;
                }
            } else {
                logger.debug("Executor {} terminated gracefully", name);
                return true;
            }

        } catch (InterruptedException e) {
            // 如果等待被中断，强制关闭
            // If waiting is interrupted, force shutdown
            logger.warn(
                    "Interrupted while waiting for executor {} termination, forcing shutdown",
                    name);
            executor.shutdownNow();
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * 关闭Closeable资源 Close a Closeable resource safely
     *
     * @param closeable Resource to close
     * @param name Name of the resource for logging
     */
    public static void closeResource(Closeable closeable, String name) {
        if (closeable == null) {
            logger.debug("Resource {} is null, nothing to close", name);
            return;
        }

        try {
            logger.debug("Closing resource: {}", name);
            closeable.close();
            logger.debug("Resource {} closed successfully", name);
        } catch (Exception e) {
            logger.error("Error closing resource: {}", name, e);
        }
    }

    /**
     * 关闭AutoCloseable资源 Close an AutoCloseable resource safely
     *
     * @param closeable Resource to close
     * @param name Name of the resource for logging
     */
    public static void closeResource(AutoCloseable closeable, String name) {
        if (closeable == null) {
            logger.debug("Resource {} is null, nothing to close", name);
            return;
        }

        try {
            logger.debug("Closing resource: {}", name);
            closeable.close();
            logger.debug("Resource {} closed successfully", name);
        } catch (Exception e) {
            logger.error("Error closing resource: {}", name, e);
        }
    }

    /**
     * 批量关闭多个资源 Close multiple resources in order
     *
     * @param resources List of resources to close
     */
    public static void closeResources(List<ResourceHolder> resources) {
        if (resources == null || resources.isEmpty()) {
            return;
        }

        logger.debug("Closing {} resources", resources.size());

        List<String> failedResources = new ArrayList<>();

        for (ResourceHolder holder : resources) {
            try {
                if (holder.resource instanceof AutoCloseable) {
                    closeResource((AutoCloseable) holder.resource, holder.name);
                } else if (holder.resource instanceof Closeable) {
                    closeResource((Closeable) holder.resource, holder.name);
                } else {
                    logger.warn("Resource {} is not closeable", holder.name);
                }
            } catch (Exception e) {
                logger.error("Error closing resource: {}", holder.name, e);
                failedResources.add(holder.name);
            }
        }

        if (!failedResources.isEmpty()) {
            logger.warn(
                    "Failed to close {} resources: {}", failedResources.size(), failedResources);
        } else {
            logger.debug("All resources closed successfully");
        }
    }

    /** 资源持有者 Holder for a resource with its name */
    public static class ResourceHolder {
        private final Object resource;
        private final String name;

        public ResourceHolder(Object resource, String name) {
            this.resource = resource;
            this.name = name;
        }

        public static ResourceHolder of(Object resource, String name) {
            return new ResourceHolder(resource, name);
        }
    }

    /**
     * 等待条件满足或超时 Wait for a condition to be met or timeout
     *
     * @param condition Condition to check
     * @param timeoutSeconds Timeout in seconds
     * @param checkIntervalMs Check interval in milliseconds
     * @param description Description for logging
     * @return true if condition met, false if timeout
     */
    public static boolean waitForCondition(
            java.util.function.BooleanSupplier condition,
            int timeoutSeconds,
            int checkIntervalMs,
            String description) {

        logger.debug("Waiting for condition: {}", description);

        long startTime = System.currentTimeMillis();
        long timeoutMs = timeoutSeconds * 1000L;

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            if (condition.getAsBoolean()) {
                logger.debug("Condition met: {}", description);
                return true;
            }

            try {
                Thread.sleep(checkIntervalMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Interrupted while waiting for condition: {}", description);
                return false;
            }
        }

        logger.warn("Timeout waiting for condition: {}", description);
        return false;
    }
}
