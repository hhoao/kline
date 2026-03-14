package com.hhoa.kline.core.core.integrations.checkpoints;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CheckpointInitializer {

    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public static CompletableFuture<Void> ensureCheckpointInitialized(
            ICheckpointManager checkpointManager, long timeoutMs, String timeoutMessage) {
        if (checkpointManager == null) {
            return CompletableFuture.completedFuture(null);
        }

        CompletableFuture<Void> initFuture;

        if (checkpointManager instanceof TaskCheckpointManager) {
            TaskCheckpointManager taskCheckpointManager = (TaskCheckpointManager) checkpointManager;
            initFuture = taskCheckpointManager.checkpointTrackerCheckAndInit().thenApply(v -> null);
        } else {
            CompletableFuture<Void> initializeFuture = checkpointManager.initialize();
            if (initializeFuture != null) {
                initFuture = initializeFuture;
            } else {
                initFuture = CompletableFuture.completedFuture(null);
            }
        }

        CompletableFuture<Void> timeoutFuture = new CompletableFuture<>();
        scheduler.schedule(
                () -> {
                    if (!initFuture.isDone()) {
                        timeoutFuture.completeExceptionally(new RuntimeException(timeoutMessage));
                    }
                },
                timeoutMs,
                TimeUnit.MILLISECONDS);

        initFuture.whenComplete(
                (result, error) -> {
                    if (error != null) {
                        timeoutFuture.completeExceptionally(error);
                    } else {
                        timeoutFuture.complete(result);
                    }
                });

        return CompletableFuture.anyOf(initFuture, timeoutFuture).thenApply(v -> null);
    }
}
