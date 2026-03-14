package com.hhoa.kline.core.core.integrations.checkpoints;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hhoa.kline.core.core.context.management.LocalContextManager;
import com.hhoa.kline.core.core.context.tracking.FileContextTracker;
import com.hhoa.kline.core.core.integrations.editor.DiffViewProvider;
import com.hhoa.kline.core.core.shared.ApiMetrics;
import com.hhoa.kline.core.core.shared.ApiMetricsUtils;
import com.hhoa.kline.core.core.shared.ClineApiReqInfo;
import com.hhoa.kline.core.core.shared.ClineSay;
import com.hhoa.kline.core.core.shared.CombineApiRequestsUtils;
import com.hhoa.kline.core.core.shared.CombineCommandSequencesUtils;
import com.hhoa.kline.core.core.shared.proto.host.ShowMessageRequest;
import com.hhoa.kline.core.core.shared.proto.host.ShowMessageType;
import com.hhoa.kline.core.core.storage.StateManager;
import com.hhoa.kline.core.core.task.ClineMessage;
import com.hhoa.kline.core.core.task.MessageStateHandler;
import com.hhoa.kline.core.core.task.MultiFileDiff;
import com.hhoa.kline.core.core.task.TaskState;
import com.hhoa.kline.core.core.workspace.WorkspaceRootManager;
import com.hhoa.kline.core.subscription.DefaultSubscriptionManager;
import com.hhoa.kline.core.subscription.message.WindowShowMessageRequestMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TaskCheckpointManager implements ICheckpointManager {

    private final String taskId;
    private final boolean enableCheckpoints;
    private final MessageStateHandler messageStateHandler;
    private final FileContextTracker fileContextTracker;
    private final DiffViewProvider diffViewProvider;
    private final TaskState taskState;
    private final WorkspaceRootManager workspaceManager;
    private final StateManager stateManager;
    private final Runnable postStateToWebview;
    private final Runnable cancelTask;
    private final CheckpointManagerCallbacks callbacks;

    private CheckpointTracker checkpointTracker;
    private CompletableFuture<CheckpointTracker> checkpointTrackerInitPromise;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private int[] conversationHistoryDeletedRange;

    public TaskCheckpointManager(
            String taskId,
            boolean enableCheckpoints,
            MessageStateHandler messageStateHandler,
            FileContextTracker fileContextTracker,
            DiffViewProvider diffViewProvider,
            TaskState taskState,
            WorkspaceRootManager workspaceManager,
            CheckpointManagerCallbacks callbacks,
            int[] initialConversationHistoryDeletedRange,
            String initialCheckpointManagerErrorMessage,
            StateManager stateManager) {
        this.taskId = taskId;
        this.enableCheckpoints = enableCheckpoints;
        this.messageStateHandler = messageStateHandler;
        this.fileContextTracker = fileContextTracker;
        this.diffViewProvider = diffViewProvider;
        this.taskState = taskState;
        this.workspaceManager = workspaceManager;
        this.stateManager = stateManager;
        this.callbacks = callbacks;
        this.postStateToWebview = callbacks.getPostStateToWebview();
        this.cancelTask = callbacks.getCancelTask();

        // Initialize state from parameters
        this.conversationHistoryDeletedRange = initialConversationHistoryDeletedRange;
        if (initialCheckpointManagerErrorMessage != null) {
            taskState.setCheckpointManagerErrorMessage(initialCheckpointManagerErrorMessage);
        }
    }

    @Override
    public CompletableFuture<Void> saveCheckpoint(
            boolean isAttemptCompletionMessage, Long completionMessageTs) {
        return CompletableFuture.runAsync(
                () -> {
                    try {
                        if (!enableCheckpoints
                                || (taskState.getCheckpointManagerErrorMessage() != null
                                        && taskState
                                                .getCheckpointManagerErrorMessage()
                                                .contains(
                                                        "Checkpoints initialization timed out."))) {
                            return;
                        }

                        List<ClineMessage> clineMessages = messageStateHandler.getClineMessages();
                        for (ClineMessage message : clineMessages) {
                            if (ClineSay.CHECKPOINT_CREATED.equals(message.getSay())) {
                                message.setIsCheckpointCheckedOut(false);
                            }
                        }

                        if (checkpointTracker == null
                                && !isAttemptCompletionMessage
                                && taskState.getCheckpointManagerErrorMessage() == null) {
                            checkpointTrackerCheckAndInit().join();
                        } else if (checkpointTracker == null
                                && isAttemptCompletionMessage
                                && (taskState.getCheckpointManagerErrorMessage() == null
                                        || !taskState
                                                .getCheckpointManagerErrorMessage()
                                                .contains(
                                                        "Checkpoints initialization timed out."))) {
                            checkpointTrackerCheckAndInit().join();
                        }

                        if (checkpointTracker == null) {
                            log.error(
                                    "Failed to save checkpoint for task {}: Checkpoint tracker not available",
                                    taskId);
                            return;
                        }

                        if (!isAttemptCompletionMessage) {
                            ClineMessage lastMessage =
                                    clineMessages.isEmpty()
                                            ? null
                                            : clineMessages.get(clineMessages.size() - 1);
                            if (lastMessage != null
                                    && ClineSay.CHECKPOINT_CREATED.equals(lastMessage.getSay())) {
                                return;
                            }

                            Long messageTs =
                                    callbacks
                                            .say(
                                                    ClineSay.CHECKPOINT_CREATED,
                                                    null,
                                                    null,
                                                    null,
                                                    null)
                                            .join();
                            if (messageTs != null) {
                                List<ClineMessage> messages =
                                        messageStateHandler.getClineMessages();
                                ClineMessage targetMessage =
                                        messages.stream()
                                                .filter(m -> m.getTs().equals(messageTs))
                                                .findFirst()
                                                .orElse(null);

                                if (targetMessage != null) {
                                    checkpointTracker
                                            .commit()
                                            .thenAccept(
                                                    commitHash -> {
                                                        if (commitHash != null) {
                                                            targetMessage.setLastCheckpointHash(
                                                                    commitHash);
                                                            try {
                                                                messageStateHandler
                                                                        .saveClineMessagesAndUpdateHistory();
                                                            } catch (Exception e) {
                                                                log.error(
                                                                        "Failed to save cline messages: {}",
                                                                        e.getMessage(),
                                                                        e);
                                                            }
                                                        }
                                                    })
                                            .exceptionally(
                                                    error -> {
                                                        log.error(
                                                                "Failed to create checkpoint commit for task {}: {}",
                                                                taskId,
                                                                error.getMessage());
                                                        return null;
                                                    });
                                }
                            }
                        } else {
                            List<ClineMessage> lastFiveMessages =
                                    clineMessages.size() > 3
                                            ? new ArrayList<>(
                                                    clineMessages.subList(
                                                            clineMessages.size() - 3,
                                                            clineMessages.size()))
                                            : new ArrayList<>(clineMessages);
                            ClineMessage lastCompletionResultMessage =
                                    lastFiveMessages.stream()
                                            .filter(
                                                    m ->
                                                            ClineSay.COMPLETION_RESULT.equals(
                                                                    m.getSay()))
                                            .reduce((first, second) -> second)
                                            .orElse(null);

                            if (lastCompletionResultMessage != null
                                    && lastCompletionResultMessage.getLastCheckpointHash()
                                            != null) {
                                log.info(
                                        "Completion checkpoint already exists, skipping duplicate checkpoint creation");
                                return;
                            }

                            if (checkpointTracker != null) {
                                String commitHash = checkpointTracker.commit().join();

                                if (completionMessageTs != null) {
                                    ClineMessage targetMessage =
                                            messageStateHandler.getClineMessages().stream()
                                                    .filter(
                                                            m ->
                                                                    m.getTs()
                                                                            .equals(
                                                                                    completionMessageTs))
                                                    .findFirst()
                                                    .orElse(null);
                                    if (targetMessage != null) {
                                        targetMessage.setLastCheckpointHash(commitHash);
                                        try {
                                            messageStateHandler.saveClineMessagesAndUpdateHistory();
                                        } catch (Exception e) {
                                            log.error(
                                                    "Failed to save cline messages: {}",
                                                    e.getMessage(),
                                                    e);
                                        }
                                    }
                                } else {
                                    if (lastCompletionResultMessage != null) {
                                        lastCompletionResultMessage.setLastCheckpointHash(
                                                commitHash);
                                        try {
                                            messageStateHandler.saveClineMessagesAndUpdateHistory();
                                        } catch (Exception e) {
                                            log.error(
                                                    "Failed to save cline messages: {}",
                                                    e.getMessage(),
                                                    e);
                                        }
                                    }
                                }
                            } else {
                                log.error(
                                        "Checkpoint tracker does not exist and could not be initialized for attempt completion for task {}",
                                        taskId);
                            }
                        }
                    } catch (Exception e) {
                        log.error(
                                "Failed to save checkpoint for task {}: {}",
                                taskId,
                                e.getMessage(),
                                e);
                    }
                });
    }

    @Override
    public CompletableFuture<Object> restoreCheckpoint(
            long messageTs, String restoreType, Integer offset) {
        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        List<ClineMessage> clineMessages = messageStateHandler.getClineMessages();
                        int messageIndex = -1;
                        for (int i = 0; i < clineMessages.size(); i++) {
                            if (clineMessages.get(i).getTs().equals(messageTs)) {
                                messageIndex = i;
                                break;
                            }
                        }
                        messageIndex -= (offset != null ? offset : 0);

                        int lastHashIndex = -1;
                        for (int i = messageIndex - 1; i >= 0; i--) {
                            if (clineMessages.get(i).getLastCheckpointHash() != null) {
                                lastHashIndex = i;
                                break;
                            }
                        }

                        ClineMessage message =
                                messageIndex >= 0 && messageIndex < clineMessages.size()
                                        ? clineMessages.get(messageIndex)
                                        : null;
                        ClineMessage lastMessageWithHash =
                                lastHashIndex >= 0 ? clineMessages.get(lastHashIndex) : null;

                        if (message == null) {
                            log.error(
                                    "Message not found for timestamp {} in task {}",
                                    messageTs,
                                    taskId);
                            return new CheckpointRestoreStateUpdate();
                        }

                        boolean didWorkspaceRestoreFail = false;

                        if ("taskAndWorkspace".equals(restoreType)
                                || "workspace".equals(restoreType)) {
                            if (!enableCheckpoints) {
                                String errorMessage = "Checkpoints are disabled in settings.";
                                log.error("{} for task {}", errorMessage, taskId);
                                showErrorMessage(errorMessage);
                                didWorkspaceRestoreFail = true;
                            } else {
                                if (checkpointTracker == null
                                        && taskState.getCheckpointManagerErrorMessage() == null) {
                                    try {
                                        String workspacePath = getWorkspacePath();
                                        checkpointTracker =
                                                CheckpointTracker.create(
                                                                taskId,
                                                                enableCheckpoints,
                                                                workspacePath)
                                                        .join();
                                        messageStateHandler.setCheckpointTracker(checkpointTracker);
                                    } catch (Exception e) {
                                        String errorMessage =
                                                e.getMessage() != null
                                                        ? e.getMessage()
                                                        : "Unknown error";
                                        log.error(
                                                "Failed to initialize checkpoint tracker for task {}: {}",
                                                taskId,
                                                errorMessage);
                                        taskState.setCheckpointManagerErrorMessage(errorMessage);
                                        showErrorMessage(errorMessage);
                                        didWorkspaceRestoreFail = true;
                                    }
                                }

                                String commitHash = null;
                                if (message.getLastCheckpointHash() != null
                                        && checkpointTracker != null) {
                                    commitHash = message.getLastCheckpointHash();
                                } else if (offset != null
                                        && lastMessageWithHash != null
                                        && lastMessageWithHash.getLastCheckpointHash() != null
                                        && checkpointTracker != null) {
                                    commitHash = lastMessageWithHash.getLastCheckpointHash();
                                } else if (offset == null
                                        && lastMessageWithHash != null
                                        && lastMessageWithHash.getLastCheckpointHash() != null
                                        && checkpointTracker != null) {
                                    log.warn(
                                            "Message {} has no checkpoint hash, falling back to previous checkpoint for task {}",
                                            messageTs,
                                            taskId);
                                    commitHash = lastMessageWithHash.getLastCheckpointHash();
                                }

                                if (commitHash != null && checkpointTracker != null) {
                                    try {
                                        checkpointTracker.resetHead(commitHash).join();
                                    } catch (Exception e) {
                                        String errorMessage =
                                                e.getMessage() != null
                                                        ? e.getMessage()
                                                        : "Unknown error";
                                        String fullErrorMessage =
                                                "Failed to restore checkpoint: " + errorMessage;
                                        log.error(
                                                "Failed to restore checkpoint for task {}: {}",
                                                taskId,
                                                errorMessage);
                                        showErrorMessage(fullErrorMessage);
                                        didWorkspaceRestoreFail = true;
                                    }
                                } else {
                                    String errorMessage =
                                            "Failed to restore checkpoint: No valid checkpoint hash found";
                                    log.error("{} for task {}", errorMessage, taskId);
                                    showErrorMessage(errorMessage);
                                    didWorkspaceRestoreFail = true;
                                }
                            }
                        }

                        CheckpointRestoreStateUpdate update = new CheckpointRestoreStateUpdate();
                        if (!didWorkspaceRestoreFail) {
                            handleSuccessfulRestore(restoreType, message, messageIndex, messageTs);
                            if (conversationHistoryDeletedRange != null) {
                                update.conversationHistoryDeletedRange =
                                        conversationHistoryDeletedRange;
                            }
                        } else {
                            sendRelinquishControlEvent();
                            if (taskState.getCheckpointManagerErrorMessage() != null) {
                                update.checkpointManagerErrorMessage =
                                        taskState.getCheckpointManagerErrorMessage();
                            }
                        }

                        return update;
                    } catch (Exception e) {
                        String errorMessage =
                                e.getMessage() != null ? e.getMessage() : "Unknown error";
                        log.error(
                                "Failed to restore checkpoint for task {}: {}",
                                taskId,
                                errorMessage);
                        sendRelinquishControlEvent();
                        CheckpointRestoreStateUpdate update = new CheckpointRestoreStateUpdate();
                        update.checkpointManagerErrorMessage = errorMessage;
                        return update;
                    }
                });
    }

    @Override
    public CompletableFuture<Boolean> doesLatestTaskCompletionHaveNewChanges() {
        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        if (!enableCheckpoints) {
                            return false;
                        }

                        List<ClineMessage> clineMessages = messageStateHandler.getClineMessages();
                        int messageIndex = -1;
                        for (int i = clineMessages.size() - 1; i >= 0; i--) {
                            if (ClineSay.COMPLETION_RESULT.equals(clineMessages.get(i).getSay())) {
                                messageIndex = i;
                                break;
                            }
                        }

                        if (messageIndex < 0) {
                            log.error("Completion message not found for task {}", taskId);
                            return false;
                        }

                        ClineMessage message = clineMessages.get(messageIndex);
                        String hash = message.getLastCheckpointHash();
                        if (hash == null) {
                            log.error(
                                    "No checkpoint hash found for completion message in task {}",
                                    taskId);
                            return false;
                        }

                        if (enableCheckpoints
                                && checkpointTracker == null
                                && taskState.getCheckpointManagerErrorMessage() == null) {
                            try {
                                String workspacePath = getWorkspacePath();
                                checkpointTracker =
                                        CheckpointTracker.create(
                                                        taskId, enableCheckpoints, workspacePath)
                                                .join();
                                messageStateHandler.setCheckpointTracker(checkpointTracker);
                            } catch (Exception e) {
                                String errorMessage =
                                        e.getMessage() != null ? e.getMessage() : "Unknown error";
                                log.error(
                                        "Failed to initialize checkpoint tracker for task {}: {}",
                                        taskId,
                                        errorMessage);
                                setCheckpointManagerErrorMessage(errorMessage);
                                return false;
                            }
                        }

                        if (checkpointTracker == null) {
                            log.error("Checkpoint tracker not available for task {}", taskId);
                            return false;
                        }

                        ClineMessage lastTaskCompletedMessage =
                                new ArrayList<>(clineMessages.subList(0, messageIndex))
                                        .stream()
                                                .filter(
                                                        m ->
                                                                ClineSay.COMPLETION_RESULT.equals(
                                                                        m.getSay()))
                                                .reduce((first, second) -> second)
                                                .orElse(null);

                        String lastTaskCompletedMessageCheckpointHash =
                                lastTaskCompletedMessage != null
                                        ? lastTaskCompletedMessage.getLastCheckpointHash()
                                        : null;

                        ClineMessage firstCheckpointMessage =
                                clineMessages.stream()
                                        .filter(m -> ClineSay.CHECKPOINT_CREATED.equals(m.getSay()))
                                        .findFirst()
                                        .orElse(null);
                        String firstCheckpointMessageCheckpointHash =
                                firstCheckpointMessage != null
                                        ? firstCheckpointMessage.getLastCheckpointHash()
                                        : null;

                        String previousCheckpointHash =
                                lastTaskCompletedMessageCheckpointHash != null
                                        ? lastTaskCompletedMessageCheckpointHash
                                        : firstCheckpointMessageCheckpointHash;

                        if (previousCheckpointHash == null) {
                            log.error("No previous checkpoint hash found for task {}", taskId);
                            return false;
                        }

                        Integer changedFilesCount =
                                checkpointTracker.getDiffCount(previousCheckpointHash, hash).join();
                        return changedFilesCount != null && changedFilesCount > 0;
                    } catch (Exception e) {
                        log.error(
                                "Failed to check for new changes in task {}: {}",
                                taskId,
                                e.getMessage(),
                                e);
                        return false;
                    }
                });
    }

    @Override
    public CompletableFuture<String> commit() {
        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        if (!enableCheckpoints) {
                            return null;
                        }

                        if (checkpointTracker == null) {
                            checkpointTrackerCheckAndInit().join();
                        }

                        if (checkpointTracker == null) {
                            log.error(
                                    "Checkpoint tracker not available for commit in task {}",
                                    taskId);
                            return null;
                        }

                        return checkpointTracker.commit().join();
                    } catch (Exception e) {
                        log.error(
                                "Failed to create checkpoint commit for task {}: {}",
                                taskId,
                                e.getMessage(),
                                e);
                        return null;
                    }
                });
    }

    @Override
    public CompletableFuture<Void> presentMultifileDiff(
            long messageTs, boolean seeNewChangesSinceLastTaskCompletion) {
        return CompletableFuture.runAsync(
                () -> {
                    try {
                        if (!enableCheckpoints) {
                            String errorMessage =
                                    "Checkpoints are disabled in settings. Cannot show diff.";
                            log.error("{} for task {}", errorMessage, taskId);
                            showInfoMessage(errorMessage);
                            sendRelinquishControlEvent();
                            return;
                        }

                        log.info(
                                "presentMultifileDiff for task {}, messageTs: {}",
                                taskId,
                                messageTs);
                        List<ClineMessage> clineMessages = messageStateHandler.getClineMessages();
                        int messageIndex = -1;
                        for (int i = 0; i < clineMessages.size(); i++) {
                            if (clineMessages.get(i).getTs().equals(messageTs)) {
                                messageIndex = i;
                                break;
                            }
                        }

                        if (messageIndex < 0) {
                            log.error(
                                    "Message not found for timestamp {} in task {}",
                                    messageTs,
                                    taskId);
                            sendRelinquishControlEvent();
                            return;
                        }

                        ClineMessage message = clineMessages.get(messageIndex);
                        String hash = message.getLastCheckpointHash();
                        if (hash == null) {
                            log.error(
                                    "No checkpoint hash found for message {} in task {}",
                                    messageTs,
                                    taskId);
                            sendRelinquishControlEvent();
                            return;
                        }

                        if (checkpointTracker == null
                                && enableCheckpoints
                                && taskState.getCheckpointManagerErrorMessage() == null) {
                            try {
                                String workspacePath = getWorkspacePath();
                                checkpointTracker =
                                        CheckpointTracker.create(
                                                        taskId, enableCheckpoints, workspacePath)
                                                .join();
                                messageStateHandler.setCheckpointTracker(checkpointTracker);
                            } catch (Exception e) {
                                String errorMessage =
                                        e.getMessage() != null ? e.getMessage() : "Unknown error";
                                log.error(
                                        "Failed to initialize checkpoint tracker for task {}: {}",
                                        taskId,
                                        errorMessage);
                                taskState.setCheckpointManagerErrorMessage(errorMessage);
                                showErrorMessage(errorMessage);
                                sendRelinquishControlEvent();
                                return;
                            }
                        }

                        if (checkpointTracker == null) {
                            String errorMessage = "Checkpoint tracker not available";
                            log.error("{} for task {}", errorMessage, taskId);
                            showErrorMessage(errorMessage);
                            sendRelinquishControlEvent();
                            return;
                        }

                        List<CheckpointTracker.FileDiff> changedFiles = null;

                        if (seeNewChangesSinceLastTaskCompletion) {
                            ClineMessage lastTaskCompletedMessage =
                                    new ArrayList<>(clineMessages.subList(0, messageIndex))
                                            .stream()
                                                    .filter(
                                                            m ->
                                                                    ClineSay.COMPLETION_RESULT
                                                                            .equals(m.getSay()))
                                                    .reduce((first, second) -> second)
                                                    .orElse(null);

                            ClineMessage firstCheckpointMessage =
                                    clineMessages.stream()
                                            .filter(
                                                    m ->
                                                            ClineSay.CHECKPOINT_CREATED.equals(
                                                                    m.getSay()))
                                            .findFirst()
                                            .orElse(null);

                            String previousCheckpointHash =
                                    lastTaskCompletedMessage != null
                                                    && lastTaskCompletedMessage
                                                                    .getLastCheckpointHash()
                                                            != null
                                            ? lastTaskCompletedMessage.getLastCheckpointHash()
                                            : (firstCheckpointMessage != null
                                                    ? firstCheckpointMessage.getLastCheckpointHash()
                                                    : null);

                            if (previousCheckpointHash == null) {
                                String errorMessage = "Unexpected error: No checkpoint hash found";
                                log.error("{} for task {}", errorMessage, taskId);
                                showErrorMessage(errorMessage);
                                sendRelinquishControlEvent();
                                return;
                            }

                            changedFiles =
                                    checkpointTracker
                                            .getDiffSet(previousCheckpointHash, hash)
                                            .join();
                            if (changedFiles == null || changedFiles.isEmpty()) {
                                showInfoMessage("No changes found");
                                sendRelinquishControlEvent();
                                return;
                            }
                        } else {
                            changedFiles = checkpointTracker.getDiffSet(hash, null).join();
                            if (changedFiles == null || changedFiles.isEmpty()) {
                                showInfoMessage("No changes found");
                                sendRelinquishControlEvent();
                                return;
                            }
                        }

                        // Convert CheckpointTracker.FileDiff to MultiFileDiff format and show
                        // Note: MultiFileDiff.showChangedFilesDiff will handle the conversion
                        // internally

                        // Use MultiFileDiff to show the diff
                        MultiFileDiff.showChangedFilesDiff(
                                messageStateHandler,
                                new CheckpointTrackerAdapter(checkpointTracker),
                                messageTs,
                                seeNewChangesSinceLastTaskCompletion);

                        sendRelinquishControlEvent();
                    } catch (Exception e) {
                        String errorMessage =
                                e.getMessage() != null ? e.getMessage() : "Unknown error";
                        log.error(
                                "Failed to present multifile diff for task {}: {}",
                                taskId,
                                errorMessage);
                        showErrorMessage("Failed to retrieve diff set: " + errorMessage);
                        sendRelinquishControlEvent();
                    }
                });
    }

    // Adapter to convert CheckpointTracker to MultiFileDiff.CheckpointTracker interface
    private static class CheckpointTrackerAdapter implements MultiFileDiff.CheckpointTracker {
        private final CheckpointTracker tracker;

        public CheckpointTrackerAdapter(CheckpointTracker tracker) {
            this.tracker = tracker;
        }

        @Override
        public List<MultiFileDiff.ChangedFile> getDiffSet(String fromHash) {
            List<CheckpointTracker.FileDiff> diffs = tracker.getDiffSet(fromHash, null).join();
            return convertToChangedFiles(diffs);
        }

        @Override
        public List<MultiFileDiff.ChangedFile> getDiffSet(String fromHash, String toHash) {
            List<CheckpointTracker.FileDiff> diffs = tracker.getDiffSet(fromHash, toHash).join();
            return convertToChangedFiles(diffs);
        }

        private List<MultiFileDiff.ChangedFile> convertToChangedFiles(
                List<CheckpointTracker.FileDiff> diffs) {
            if (diffs == null) {
                return List.of();
            }
            return diffs.stream()
                    .map(
                            f -> {
                                MultiFileDiff.ChangedFile cf = new MultiFileDiff.ChangedFile();
                                cf.setRelativePath(f.getRelativePath());
                                cf.setAbsolutePath(f.getAbsolutePath());
                                cf.setBefore(f.getBefore());
                                cf.setAfter(f.getAfter());
                                return cf;
                            })
                    .toList();
        }
    }

    @Override
    public CompletableFuture<Void> initialize() {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Object> checkpointTrackerCheckAndInit() {
        if (checkpointTracker != null) {
            return CompletableFuture.completedFuture(checkpointTracker);
        }

        if (checkpointTrackerInitPromise != null) {
            return checkpointTrackerInitPromise.thenApply(t -> t);
        }

        checkpointTrackerInitPromise = initializeCheckpointTracker();

        return checkpointTrackerInitPromise.thenApply(
                tracker -> {
                    checkpointTrackerInitPromise = null;
                    return tracker;
                });
    }

    private CompletableFuture<CheckpointTracker> initializeCheckpointTracker() {
        CompletableFuture<CheckpointTracker> future = new CompletableFuture<>();

        scheduler.schedule(
                () -> {
                    if (!future.isDone()) {
                        setCheckpointManagerErrorMessage(
                                "Checkpoints are taking longer than expected to initialize. Working in a large repository? Consider re-opening Cline in a project that uses git, or disabling checkpoints.");
                    }
                },
                7,
                TimeUnit.SECONDS);

        CompletableFuture<CheckpointTracker> timeoutFuture = new CompletableFuture<>();
        scheduler.schedule(
                () -> {
                    if (!future.isDone()) {
                        timeoutFuture.completeExceptionally(
                                new RuntimeException(
                                        "Checkpoints taking too long to initialize. Consider re-opening Cline in a project that uses git, or disabling checkpoints."));
                    }
                },
                15,
                TimeUnit.SECONDS);

        try {
            String workspacePath = getWorkspacePath();
            CheckpointTracker.create(taskId, enableCheckpoints, workspacePath)
                    .thenAccept(
                            tracker -> {
                                checkpointTracker = tracker;
                                if (tracker != null) {
                                    messageStateHandler.setCheckpointTracker(tracker);
                                }
                                future.complete(tracker);
                            })
                    .exceptionally(
                            error -> {
                                String errorMessage =
                                        error.getMessage() != null
                                                ? error.getMessage()
                                                : "Unknown error";
                                log.error(
                                        "Failed to initialize checkpoint tracker: {}",
                                        errorMessage);

                                if (errorMessage.contains(
                                        "Checkpoints taking too long to initialize")) {
                                    setCheckpointManagerErrorMessage(
                                            "Checkpoints initialization timed out. Consider re-opening Cline in a project that uses git, or disabling checkpoints.");
                                } else {
                                    setCheckpointManagerErrorMessage(errorMessage);
                                }
                                future.complete(null);
                                return null;
                            });
        } catch (Exception e) {
            String errorMessage = e.getMessage() != null ? e.getMessage() : "Unknown error";
            setCheckpointManagerErrorMessage(errorMessage);
            future.complete(null);
        }

        return CompletableFuture.anyOf(future, timeoutFuture)
                .thenApply(v -> checkpointTracker)
                .exceptionally(
                        error -> {
                            setCheckpointManagerErrorMessage(
                                    "Checkpoints initialization timed out. Consider re-opening Cline in a project that uses git, or disabling checkpoints.");
                            return null;
                        });
    }

    private void setCheckpointManagerErrorMessage(String errorMessage) {
        taskState.setCheckpointManagerErrorMessage(errorMessage);
        try {
            postStateToWebview.run();
        } catch (Exception e) {
            log.error("Failed to post state to webview after checkpoint error: {}", e.getMessage());
        }
    }

    private void handleSuccessfulRestore(
            String restoreType, ClineMessage message, int messageIndex, long messageTs) {
        try {
            if ("task".equals(restoreType) || "taskAndWorkspace".equals(restoreType)) {
                // Update conversation history deleted range in our state
                conversationHistoryDeletedRange = message.getConversationHistoryDeletedRange();
                taskState.setConversationHistoryDeletedRange(
                        message.getConversationHistoryDeletedRange());

                // Truncate API conversation history
                var apiConversationHistory = messageStateHandler.getApiConversationHistory();
                int conversationHistoryIndex =
                        message.getConversationHistoryIndex() != null
                                ? message.getConversationHistoryIndex()
                                : 0;
                var newConversationHistory =
                        apiConversationHistory.subList(
                                0,
                                Math.min(
                                        conversationHistoryIndex + 2,
                                        apiConversationHistory.size()));
                messageStateHandler.overwriteApiConversationHistory(newConversationHistory);

                // Update the context history state
                String taskDirectory = stateManager.getOrCreateTaskDirectoryExists(taskId);
                LocalContextManager contextManager = new LocalContextManager(taskDirectory);
                contextManager.truncateContextHistory(messageTs);

                // Aggregate deleted API reqs info so we don't lose costs/tokens
                var clineMessages = messageStateHandler.getClineMessages();
                var deletedMessages =
                        clineMessages.subList(
                                Math.min(messageIndex + 1, clineMessages.size()),
                                clineMessages.size());
                var combinedCommands =
                        CombineCommandSequencesUtils.combineCommandSequences(deletedMessages);
                var combinedApiRequests =
                        CombineApiRequestsUtils.combineApiRequests(combinedCommands);
                ApiMetrics deletedApiReqsMetrics =
                        ApiMetricsUtils.getApiMetrics(combinedApiRequests);

                // Detect files edited after this message timestamp for file context warning
                // Only needed for task-only restores when a user edits a message or restores the
                // task context, but not the files.
                if ("task".equals(restoreType)) {
                    var filesEditedAfterMessage =
                            fileContextTracker.detectFilesEditedAfterMessage(
                                    messageTs, deletedMessages);
                    if (!filesEditedAfterMessage.isEmpty()) {
                        fileContextTracker.storePendingFileContextWarning(filesEditedAfterMessage);
                    }
                }

                // Overwrite Cline messages
                var newClineMessages =
                        clineMessages.subList(0, Math.min(messageIndex + 1, clineMessages.size()));
                messageStateHandler.overwriteClineMessages(newClineMessages);

                // Send deleted_api_reqs message
                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    ClineApiReqInfo apiReqInfo = new ClineApiReqInfo();
                    apiReqInfo.setTokensIn((int) deletedApiReqsMetrics.getTotalTokensIn());
                    apiReqInfo.setTokensOut((int) deletedApiReqsMetrics.getTotalTokensOut());
                    apiReqInfo.setCacheWrites(
                            deletedApiReqsMetrics.getTotalCacheWrites() != null
                                    ? deletedApiReqsMetrics.getTotalCacheWrites().intValue()
                                    : null);
                    apiReqInfo.setCacheReads(
                            deletedApiReqsMetrics.getTotalCacheReads() != null
                                    ? deletedApiReqsMetrics.getTotalCacheReads().intValue()
                                    : null);
                    apiReqInfo.setCost(deletedApiReqsMetrics.getTotalCost());

                    String apiReqInfoJson = objectMapper.writeValueAsString(apiReqInfo);
                    callbacks
                            .say(ClineSay.DELETED_API_REQS, apiReqInfoJson, null, null, null)
                            .join();
                } catch (Exception e) {
                    log.error("Failed to send deleted_api_reqs message: {}", e.getMessage(), e);
                }
            }

            // Show success messages
            switch (restoreType) {
                case "task":
                    showInfoMessage("Task messages have been restored to the checkpoint");
                    break;
                case "workspace":
                    showInfoMessage("Workspace files have been restored to the checkpoint");
                    break;
                case "taskAndWorkspace":
                    showInfoMessage("Task and workspace have been restored to the checkpoint");
                    break;
            }

            if (!"task".equals(restoreType)) {
                // Set isCheckpointCheckedOut flag on the message
                // Find all checkpoint messages before this one
                var checkpointMessages =
                        messageStateHandler.getClineMessages().stream()
                                .filter(m -> ClineSay.CHECKPOINT_CREATED.equals(m.getSay()))
                                .toList();
                int currentMessageIndex = -1;
                for (int i = 0; i < checkpointMessages.size(); i++) {
                    if (checkpointMessages.get(i).getTs().equals(messageTs)) {
                        currentMessageIndex = i;
                        break;
                    }
                }

                // Set isCheckpointCheckedOut to false for all checkpoint messages
                for (int i = 0; i < checkpointMessages.size(); i++) {
                    checkpointMessages.get(i).setIsCheckpointCheckedOut(i == currentMessageIndex);
                }
            }

            messageStateHandler.saveClineMessagesAndUpdateHistory();

            // Cancel and reinitialize the task to get updated messages
            cancelTask.run();
        } catch (Exception e) {
            log.error("Failed to handle successful restore: {}", e.getMessage(), e);
        }
    }

    private String getWorkspacePath() {
        var primaryRoot = workspaceManager.getPrimaryRoot();
        return primaryRoot.getPath();
    }

    private void showErrorMessage(String message) {
        ShowMessageRequest request =
                ShowMessageRequest.builder().type(ShowMessageType.ERROR).message(message).build();
        DefaultSubscriptionManager.getInstance().send(new WindowShowMessageRequestMessage(request));
    }

    private void showInfoMessage(String message) {
        ShowMessageRequest request =
                ShowMessageRequest.builder()
                        .type(ShowMessageType.INFORMATION)
                        .message(message)
                        .build();
        DefaultSubscriptionManager.getInstance().send(new WindowShowMessageRequestMessage(request));
    }

    private void sendRelinquishControlEvent() {
        // TODO: Implement sendRelinquishControlEvent equivalent
        // This is a placeholder - the actual implementation depends on the Java equivalent
        // of sendRelinquishControlEvent from TypeScript
        log.debug("Relinquish control event sent");
    }

    /** Provides public read-only access to current state */
    public CheckpointManagerState getCurrentState() {
        CheckpointManagerState state = new CheckpointManagerState();
        state.conversationHistoryDeletedRange = conversationHistoryDeletedRange;
        state.checkpointTracker = checkpointTracker;
        state.checkpointManagerErrorMessage = taskState.getCheckpointManagerErrorMessage();
        return state;
    }

    /** Updates the conversation history deleted range */
    public void updateConversationHistoryDeletedRange(int[] range) {
        this.conversationHistoryDeletedRange = range;
        // TODO - Future telemetry event capture here
    }

    public static class CheckpointManagerState {
        public int[] conversationHistoryDeletedRange;
        public CheckpointTracker checkpointTracker;
        public String checkpointManagerErrorMessage;
    }

    public static class CheckpointRestoreStateUpdate {
        public int[] conversationHistoryDeletedRange;
        public String checkpointManagerErrorMessage;
    }

    public interface CheckpointManagerCallbacks {
        CompletableFuture<Long> say(
                ClineSay type,
                String text,
                List<String> images,
                List<String> files,
                Boolean partial);

        Runnable getPostStateToWebview();

        Runnable getCancelTask();
    }
}
