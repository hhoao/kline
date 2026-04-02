package com.hhoa.kline.core.core.task.handler;

import com.hhoa.ai.kline.commons.utils.JsonUtils;
import com.hhoa.kline.core.core.assistant.MessageParam;
import com.hhoa.kline.core.core.context.tracking.EnvironmentContextTracker;
import com.hhoa.kline.core.core.shared.ClineApiReqInfo;
import com.hhoa.kline.core.core.hooks.HookExecutionResult;
import com.hhoa.kline.core.core.shared.ClineAsk;
import com.hhoa.kline.core.core.shared.ClineMessageType;
import com.hhoa.kline.core.core.shared.ClineSay;
import com.hhoa.kline.core.core.storage.StateManager;
import com.hhoa.kline.core.core.task.AskPending;
import com.hhoa.kline.core.core.task.ClineMessage;
import com.hhoa.kline.core.core.task.MessageStateHandler;
import com.hhoa.kline.core.core.task.TaskHookSupport;
import com.hhoa.kline.core.core.task.TaskState;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class TaskV2ResumeHandler {

    private final StateManager stateManager;
    private final String taskId;
    private final TaskState taskState;
    private final MessageStateHandler messageStateHandler;
    private final TaskV2SayAskHandler sayAskHandler;
    private final TaskHookSupport hookSupport;
    private final EnvironmentContextTracker environmentContextTracker;
    private final Supplier<String> versionSupplier;

    public TaskV2ResumeHandler(
            StateManager stateManager,
            String taskId,
            TaskState taskState,
            MessageStateHandler messageStateHandler,
            TaskV2SayAskHandler sayAskHandler,
            TaskHookSupport hookSupport,
            EnvironmentContextTracker environmentContextTracker,
            Supplier<String> versionSupplier) {
        this.stateManager = stateManager;
        this.taskId = taskId;
        this.taskState = taskState;
        this.messageStateHandler = messageStateHandler;
        this.sayAskHandler = sayAskHandler;
        this.hookSupport = hookSupport;
        this.environmentContextTracker = environmentContextTracker;
        this.versionSupplier = versionSupplier;
    }

    /**
     * 从历史记录恢复任务。返回的 AskPending 中 isCancelled() 为 true 表示被 hook 取消。
     */
    public AskPending resumeTaskFromHistory() {
        List<ClineMessage> savedClineMessages = stateManager.getSavedClineMessages(taskId);

        int lastRelevantIndex = -1;
        for (int i = savedClineMessages.size() - 1; i >= 0; i--) {
            ClineMessage msg = savedClineMessages.get(i);
            if (!ClineAsk.RESUME_TASK.equals(msg.getAsk())
                    && !ClineAsk.RESUME_COMPLETED_TASK.equals(msg.getAsk())) {
                lastRelevantIndex = i;
                break;
            }
        }
        if (lastRelevantIndex != -1 && lastRelevantIndex < savedClineMessages.size() - 1) {
            savedClineMessages =
                    new ArrayList<>(savedClineMessages.subList(0, lastRelevantIndex + 1));
        }

        int lastApiReqStartedIndex = -1;
        for (int i = savedClineMessages.size() - 1; i >= 0; i--) {
            ClineMessage msg = savedClineMessages.get(i);
            if (ClineMessageType.SAY.equals(msg.getType())
                    && ClineSay.API_REQ_STARTED.equals(msg.getSay())) {
                lastApiReqStartedIndex = i;
                break;
            }
        }
        if (lastApiReqStartedIndex != -1) {
            ClineMessage lastApiReqStarted = savedClineMessages.get(lastApiReqStartedIndex);
            try {
                String text = lastApiReqStarted.getText();
                ClineApiReqInfo apiReqInfo =
                        JsonUtils.readValueWithException(text, ClineApiReqInfo.class);
                if (apiReqInfo.getCost() == null && apiReqInfo.getCancelReason() == null) {
                    savedClineMessages.remove(lastApiReqStartedIndex);
                }
            } catch (Exception e) {
                savedClineMessages.remove(lastApiReqStartedIndex);
            }
        }

        messageStateHandler.overwriteClineMessages(savedClineMessages);

        List<MessageParam> savedApiConversationHistory =
                stateManager.getSavedApiConversationHistory(taskId);
        messageStateHandler.setApiConversationHistory(savedApiConversationHistory);

        ClineMessage lastClineMessage = null;
        for (int i = savedClineMessages.size() - 1; i >= 0; i--) {
            ClineMessage msg = savedClineMessages.get(i);
            if (!ClineAsk.RESUME_TASK.equals(msg.getAsk())
                    && !ClineAsk.RESUME_COMPLETED_TASK.equals(msg.getAsk())) {
                lastClineMessage = msg;
                break;
            }
        }

        ClineAsk askType;
        if (lastClineMessage != null
                && ClineAsk.COMPLETION_RESULT.equals(lastClineMessage.getAsk())) {
            askType = ClineAsk.RESUME_COMPLETED_TASK;
        } else {
            askType = ClineAsk.RESUME_TASK;
        }

        if (environmentContextTracker != null) {
            environmentContextTracker.recordEnvironment(
                    versionSupplier != null ? versionSupplier.get() : "unknown");
        }

        taskState.setInitialized(true);
        taskState.setAbort(false); // Reset abort flag when resuming task

        AskPending askPending = sayAskHandler.ask(askType, null);
        askPending.setAskType(askType);

        // Execute TaskResume hook AFTER user clicks resume button
        if (hookSupport != null) {
            Long lastTs = lastClineMessage != null ? lastClineMessage.getTs() : null;
            int messageCount = savedClineMessages.size();
            boolean historyDeleted = taskState.getConversationHistoryDeletedRange() != null;

            HookExecutionResult resumeResult =
                    hookSupport.executeTaskResumeHook(lastTs, messageCount, historyDeleted);

            if (resumeResult.getCancel() != null && resumeResult.getCancel()) {
                hookSupport.handleHookCancellation("TaskResume", resumeResult.isWasCancelled());
                askPending.setCancelled(true);
                return askPending;
            }

            // Inject hook context modification into askPending for downstream use
            if (resumeResult.getContextModification() != null
                    && !resumeResult.getContextModification().trim().isEmpty()) {
                askPending.setHookContextModification(
                        "<hook_context source=\"TaskResume\" type=\"general\">\n"
                                + resumeResult.getContextModification().trim()
                                + "\n</hook_context>");
            }
        }

        // Defensive check
        if (taskState.isAbort()) {
            askPending.setCancelled(true);
            return askPending;
        }

        return askPending;
    }
}
