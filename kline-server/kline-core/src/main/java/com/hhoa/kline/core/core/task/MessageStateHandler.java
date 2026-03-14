package com.hhoa.kline.core.core.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hhoa.kline.core.common.Tuple2;
import com.hhoa.kline.core.core.assistant.MessageParam;
import com.hhoa.kline.core.core.controller.HistoryItem;
import com.hhoa.kline.core.core.integrations.checkpoints.CheckpointTracker;
import com.hhoa.kline.core.core.shared.ClineApiReqInfo;
import com.hhoa.kline.core.core.shared.ClineAsk;
import com.hhoa.kline.core.core.storage.StateManager;
import com.hhoa.kline.core.core.workspace.WorkspaceRootManager;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
public class MessageStateHandler {

    private String taskId;
    private String ulid;
    private TaskState taskState;
    private List<ClineMessage> clineMessages = new CopyOnWriteArrayList<>();

    private List<MessageParam> apiConversationHistory = new ArrayList<>();

    private boolean taskIsFavorited = false;

    private CheckpointTracker checkpointTracker;

    private StateManager stateManager;
    private final WorkspaceRootManager workspaceRootManager;
    private ObjectMapper objectMapper = new ObjectMapper();

    public MessageStateHandler(
            String taskId,
            String ulid,
            TaskState taskState,
            StateManager stateManager,
            WorkspaceRootManager workspaceRootManager) {
        this.taskId = taskId;
        this.ulid = ulid;
        this.taskState = taskState;
        this.stateManager = stateManager;
        this.workspaceRootManager = workspaceRootManager;
    }

    public void setClineMessages(List<ClineMessage> newMessages) {
        clineMessages.clear();
        if (newMessages != null) {
            clineMessages.addAll(newMessages);
        }
    }

    public void addToClineMessages(ClineMessage message) {
        if (message == null) {
            return;
        }

        if (taskState != null) {
            message.setConversationHistoryIndex(
                    apiConversationHistory.isEmpty() ? -1 : apiConversationHistory.size() - 1);
            message.setConversationHistoryDeletedRange(
                    taskState.getConversationHistoryDeletedRange());
        }

        clineMessages.add(message);
        saveClineMessagesAndUpdateHistory();
    }

    public void updateClineMessage(int index, ClineMessage updates) {
        if (index < 0 || index >= clineMessages.size()) {
            log.error("Invalid message index: {}", index);
            return;
        }
        ClineMessage current = clineMessages.get(index);

        if (updates.getText() != null) {
            current.setText(updates.getText());
        }
        if (updates.getTs() != null) {
            current.setTs(updates.getTs());
        }
        if (updates.getType() != null) {
            current.setType(updates.getType());
        }
        if (updates.getAsk() != null) {
            current.setAsk(updates.getAsk());
        }
        if (updates.getSay() != null) {
            current.setSay(updates.getSay());
        }
        if (updates.getReasoning() != null) {
            current.setReasoning(updates.getReasoning());
        }
        if (updates.getImages() != null) {
            current.setImages(updates.getImages());
        }
        if (updates.getFiles() != null) {
            current.setFiles(updates.getFiles());
        }
        if (updates.getPartial() != null) {
            current.setPartial(updates.getPartial());
        }
        if (updates.getCommandCompleted() != null) {
            current.setCommandCompleted(updates.getCommandCompleted());
        }
        if (updates.getLastCheckpointHash() != null) {
            current.setLastCheckpointHash(updates.getLastCheckpointHash());
        }
        if (updates.getIsCheckpointCheckedOut() != null) {
            current.setIsCheckpointCheckedOut(updates.getIsCheckpointCheckedOut());
        }
        if (updates.getIsOperationOutsideWorkspace() != null) {
            current.setIsOperationOutsideWorkspace(updates.getIsOperationOutsideWorkspace());
        }
        if (updates.getConversationHistoryIndex() != null) {
            current.setConversationHistoryIndex(updates.getConversationHistoryIndex());
        }
        if (updates.getConversationHistoryDeletedRange() != null) {
            current.setConversationHistoryDeletedRange(
                    updates.getConversationHistoryDeletedRange());
        }

        saveClineMessagesAndUpdateHistory();
    }

    public void overwriteClineMessages(List<ClineMessage> newMessages) {
        setClineMessages(newMessages);
        saveClineMessagesAndUpdateHistory();
    }

    public void saveClineMessagesAndUpdateHistory() {
        if (taskId == null) {
            log.error("No taskId available to save messages");
            return;
        }

        try {
            saveClineMessagesToDisk();

            List<ClineMessage> messagesToProcess =
                    clineMessages.isEmpty()
                            ? new ArrayList<>()
                            : clineMessages.subList(1, clineMessages.size());
            List<ClineMessage> combinedCommands =
                    MessageUtils.combineCommandSequences(messagesToProcess);
            List<ClineMessage> combinedApiRequests =
                    MessageUtils.combineApiRequests(combinedCommands);
            Tuple2<Integer, ClineApiReqInfo> apiReqInfoTuple =
                    MessageUtils.getApiReqInfo(combinedApiRequests);

            ClineApiReqInfo apiReqInfo = apiReqInfoTuple.f1;

            ClineMessage taskMessage = clineMessages.isEmpty() ? null : clineMessages.getFirst();

            ClineMessage lastRelevantMessage =
                    MessageUtils.findLastIndex(
                                            clineMessages,
                                            msg ->
                                                    !(ClineAsk.RESUME_TASK.equals(msg.getAsk())
                                                            || ClineAsk.RESUME_COMPLETED_TASK
                                                                    .equals(msg.getAsk())))
                                    >= 0
                            ? clineMessages.get(
                                    MessageUtils.findLastIndex(
                                            clineMessages,
                                            msg ->
                                                    !(ClineAsk.RESUME_TASK.equals(msg.getAsk())
                                                            || ClineAsk.RESUME_COMPLETED_TASK
                                                                    .equals(msg.getAsk()))))
                            : null;

            long taskDirSize = stateManager.getTaskDirectorySize(taskId);

            String shadowGitConfigWorkTree = null;
            if (checkpointTracker != null) {
                try {
                    shadowGitConfigWorkTree = checkpointTracker.getShadowGitConfigWorkTree().get();
                } catch (Exception e) {
                    log.debug("Failed to get shadow git config worktree: {}", e.getMessage());
                }
            }

            HistoryItem historyItem =
                    HistoryItem.builder()
                            .id(taskId)
                            .ulid(ulid)
                            .ts(
                                    lastRelevantMessage != null
                                                    && lastRelevantMessage.getTs() != null
                                            ? lastRelevantMessage.getTs()
                                            : System.currentTimeMillis())
                            .task(
                                    taskMessage != null && taskMessage.getText() != null
                                            ? taskMessage.getText()
                                            : "")
                            .tokensIn(apiReqInfo.getTokensIn())
                            .tokensOut(apiReqInfo.getTokensOut())
                            .cacheWrites(apiReqInfo.getCacheWrites())
                            .cacheReads(apiReqInfo.getCacheReads())
                            .totalCost(apiReqInfo.getCost())
                            .size(taskDirSize)
                            .shadowGitConfigWorkTree(shadowGitConfigWorkTree)
                            .cwdOnTaskInitialization(workspaceRootManager.getCwd())
                            .conversationHistoryDeletedRange(
                                    taskState != null
                                                    && taskState
                                                                    .getConversationHistoryDeletedRange()
                                                            != null
                                            ? new int[] {
                                                taskState.getConversationHistoryDeletedRange()[0],
                                                taskState.getConversationHistoryDeletedRange()[1]
                                            }
                                            : null)
                            .favorited(taskIsFavorited)
                            .checkpointManagerErrorMessage(
                                    taskState != null
                                            ? taskState.getCheckpointManagerErrorMessage()
                                            : null)
                            .build();

            updateTaskHistory(historyItem);

        } catch (Exception error) {
            log.error("Failed to save cline messages and update history", error);
        }
    }

    private void saveClineMessagesToDisk() {
        if (taskId == null) {
            return;
        }
        stateManager.saveClineMessages(taskId, clineMessages);
    }

    private void updateTaskHistory(HistoryItem item) {
        try {
            List<HistoryItem> history = this.stateManager.getGlobalState().getTaskHistory();
            if (history == null) {
                history = new ArrayList<>();
            }

            int existingIndex = -1;
            for (int i = 0; i < history.size(); i++) {
                if (item.getId().equals(history.get(i).getId())) {
                    existingIndex = i;
                    break;
                }
            }

            if (existingIndex >= 0) {
                history.set(existingIndex, item);
            } else {
                history.add(item);
            }

            this.stateManager.getGlobalState().setTaskHistory(history);
        } catch (Exception e) {
            log.error("Failed to update task history", e);
        }
    }

    public void addToApiConversationHistory(MessageParam message) {
        if (message != null) {
            apiConversationHistory.add(message);
            saveApiConversationHistoryToDisk();
        }
    }

    public void setApiConversationHistory(List<MessageParam> newHistory) {
        apiConversationHistory.clear();
        if (newHistory != null) {
            apiConversationHistory.addAll(newHistory);
        }
    }

    public void overwriteApiConversationHistory(List<MessageParam> newHistory) {
        setApiConversationHistory(newHistory);
        saveApiConversationHistoryToDisk();
    }

    private void saveApiConversationHistoryToDisk() {
        if (taskId == null) {
            return;
        }
        try {
            List<MessageParam> historyAsMaps = new ArrayList<>(apiConversationHistory);
            stateManager.saveApiConversationHistory(taskId, historyAsMaps);
        } catch (Exception e) {
            log.error("Failed to save API conversation history to disk", e);
        }
    }
}
