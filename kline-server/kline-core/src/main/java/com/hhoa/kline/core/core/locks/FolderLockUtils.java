package com.hhoa.kline.core.core.locks;

import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;

/**
 * 文件夹锁工具类，提供带重试的锁获取和释放。
 *
 * <p>对应 Cline TS 版本的 FolderLockUtils.ts。
 */
@Slf4j
public class FolderLockUtils {

    private FolderLockUtils() {}

    /**
     * 尝试带重试地获取文件夹锁。
     *
     * @param lockManagerSupplier 获取 LockManager 的供应商（可能为 null）
     * @param options 锁参数
     * @param config 重试配置（null 则使用默认值）
     * @return 获取结果
     */
    public static FolderLockWithRetryResult tryAcquireFolderLockWithRetry(
            Supplier<LockManager> lockManagerSupplier,
            FolderLockOptions options,
            FolderLockRetryConfig config) {
        return retryFolderLockAcquisition(
                () -> {
                    try {
                        LockManager lockManager =
                                lockManagerSupplier != null ? lockManagerSupplier.get() : null;
                        if (lockManager == null) {
                            log.debug("Lock manager not available - skipping lock acquisition");
                            return FolderLockWithRetryResult.builder()
                                    .acquired(false)
                                    .skipped(true)
                                    .build();
                        }

                        log.info(
                                "Attempting to acquire folder lock for: {}",
                                options.getLockTarget());

                        FolderLockResult result = acquireFolderLock(lockManager, options);
                        return FolderLockWithRetryResult.builder()
                                .acquired(result.isAcquired())
                                .conflictingLock(result.getConflictingLock())
                                .skipped(false)
                                .build();
                    } catch (Exception e) {
                        log.error("Error in folder lock acquisition attempt:", e);
                        return FolderLockWithRetryResult.builder().acquired(false).build();
                    }
                },
                config);
    }

    /** 释放文件夹锁。 */
    public static void releaseFolderLock(
            Supplier<LockManager> lockManagerSupplier, String taskId, String lockTarget) {
        try {
            LockManager lockManager =
                    lockManagerSupplier != null ? lockManagerSupplier.get() : null;
            if (lockManager == null) {
                log.debug("Lock manager not available - skipping lock release");
                return;
            }
            lockManager.releaseFolderLockByTarget(taskId, lockTarget);
            log.info("Released folder lock for: {}", lockTarget);
        } catch (Exception e) {
            log.error("Error releasing folder lock:", e);
        }
    }

    /** 无重试地获取文件夹锁。 */
    public static FolderLockResult acquireFolderLock(
            LockManager lockManager, FolderLockOptions options) {
        if (lockManager == null) {
            log.debug("Lock manager not available - cannot acquire folder lock");
            return FolderLockResult.builder().acquired(false).build();
        }

        try {
            LockRow conflicting =
                    lockManager.registerFolderLock(options.getHeldBy(), options.getLockTarget());
            if (conflicting == null) {
                return FolderLockResult.builder().acquired(true).build();
            } else {
                return FolderLockResult.builder()
                        .acquired(false)
                        .conflictingLock(conflicting)
                        .build();
            }
        } catch (Exception e) {
            log.error("Failed to acquire folder lock:", e);
            return FolderLockResult.builder().acquired(false).build();
        }
    }

    /**
     * 带退避的重试获取。
     *
     * @param operation 尝试获取锁的操作
     * @param config 重试配置（null 则使用默认值）
     */
    public static FolderLockWithRetryResult retryFolderLockAcquisition(
            Supplier<FolderLockWithRetryResult> operation, FolderLockRetryConfig config) {
        if (config == null) {
            config = FolderLockRetryConfig.defaultConfig();
        }

        long startTime = System.currentTimeMillis();
        int attemptCount = 0;
        FolderLockWithRetryResult lastResult = null;

        while (true) {
            long elapsedTime = System.currentTimeMillis() - startTime;

            if (elapsedTime >= config.getMaxTotalTimeoutMs()) {
                log.warn(
                        "Folder lock acquisition timed out after {}ms",
                        config.getMaxTotalTimeoutMs());
                return lastResult != null
                        ? lastResult
                        : FolderLockWithRetryResult.builder().acquired(false).build();
            }

            try {
                FolderLockWithRetryResult result = operation.get();
                lastResult = result;

                if (result.isSkipped() || result.isAcquired()) {
                    if (result.isAcquired() && attemptCount > 0) {
                        log.debug(
                                "Folder lock acquired after {} attempts ({}ms)",
                                attemptCount + 1,
                                elapsedTime);
                    }
                    return result;
                }
            } catch (Exception e) {
                log.error("Error during folder lock acquisition attempt {}:", attemptCount + 1, e);
            }

            attemptCount++;
            int baseDelay =
                    config.getInitialDelayMs() + attemptCount * config.getIncrementPerAttemptMs();
            long remainingTime =
                    config.getMaxTotalTimeoutMs() - (System.currentTimeMillis() - startTime);
            int delay = (int) Math.min(baseDelay, Math.max(0, remainingTime));

            if (delay <= 0) {
                log.warn(
                        "Folder lock acquisition timed out after {}ms",
                        config.getMaxTotalTimeoutMs());
                return lastResult != null
                        ? lastResult
                        : FolderLockWithRetryResult.builder().acquired(false).build();
            }

            log.info(
                    "Folder lock held by another instance, retrying in {}ms (attempt {})",
                    delay,
                    attemptCount);
            try {
                Thread.sleep(delay);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return lastResult != null
                        ? lastResult
                        : FolderLockWithRetryResult.builder().acquired(false).build();
            }
        }
    }
}
