package com.hhoa.kline.core.core.integrations.checkpoints;

import java.util.concurrent.CompletableFuture;

public interface ICheckpointManager {

    CompletableFuture<Void> saveCheckpoint(
            boolean isAttemptCompletionMessage, Long completionMessageTs);

    CompletableFuture<Object> restoreCheckpoint(long messageTs, String restoreType, Integer offset);

    CompletableFuture<Boolean> doesLatestTaskCompletionHaveNewChanges();

    CompletableFuture<String> commit();

    CompletableFuture<Void> presentMultifileDiff(
            long messageTs, boolean seeNewChangesSinceLastTaskCompletion);

    CompletableFuture<Void> initialize();

    CompletableFuture<Object> checkpointTrackerCheckAndInit();
}
