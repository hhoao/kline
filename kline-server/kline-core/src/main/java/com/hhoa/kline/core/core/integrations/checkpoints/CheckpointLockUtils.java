package com.hhoa.kline.core.core.integrations.checkpoints;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CheckpointLockUtils {

    private static final String CHECKPOINTS_BASE_PATH = "~/.cline/data/checkpoints";
    private static final Map<String, String> CHECKPOINT_LOCKS = new ConcurrentHashMap<>();

    public record FolderLockWithRetryResult(
            boolean acquired, boolean skipped, String conflictingLock) {}

    public static FolderLockWithRetryResult tryAcquireCheckpointLockWithRetry(
            String cwdHash, String taskId) {
        String lockTarget = CHECKPOINTS_BASE_PATH + "/" + cwdHash;

        String existingHolder = CHECKPOINT_LOCKS.computeIfAbsent(lockTarget, k -> taskId);

        if (existingHolder.equals(taskId)) {
            return new FolderLockWithRetryResult(true, false, null);
        } else {
            return new FolderLockWithRetryResult(false, false, existingHolder);
        }
    }

    public static void releaseCheckpointLock(String cwdHash, String taskId) {
        String lockTarget = CHECKPOINTS_BASE_PATH + "/" + cwdHash;
        String currentHolder = CHECKPOINT_LOCKS.get(lockTarget);

        if (currentHolder != null && currentHolder.equals(taskId)) {
            CHECKPOINT_LOCKS.remove(lockTarget);
        }
    }
}
