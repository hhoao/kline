package com.hhoa.kline.core.core.task.handler;

import com.hhoa.ai.kline.commons.utils.JsonUtils;
import com.hhoa.kline.core.core.assistant.MessageParam;
import com.hhoa.kline.core.core.shared.ClineApiReqInfo;
import com.hhoa.kline.core.core.shared.ClineAsk;
import com.hhoa.kline.core.core.shared.ClineMessageType;
import com.hhoa.kline.core.core.shared.ClineSay;
import com.hhoa.kline.core.core.storage.StateManager;
import com.hhoa.kline.core.core.task.AskPending;
import com.hhoa.kline.core.core.task.ClineMessage;
import com.hhoa.kline.core.core.task.MessageStateHandler;
import com.hhoa.kline.core.core.task.TaskState;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class TaskV2ResumeHandler {

    private final StateManager stateManager;
    private final String taskId;
    private final TaskState taskState;
    private final MessageStateHandler messageStateHandler;
    private final TaskV2SayAskHandler sayAskHandler;

    public TaskV2ResumeHandler(
            StateManager stateManager,
            String taskId,
            TaskState taskState,
            MessageStateHandler messageStateHandler,
            TaskV2SayAskHandler sayAskHandler) {
        this.stateManager = stateManager;
        this.taskId = taskId;
        this.taskState = taskState;
        this.messageStateHandler = messageStateHandler;
        this.sayAskHandler = sayAskHandler;
    }

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

        taskState.setInitialized(true);

        AskPending askPending = sayAskHandler.ask(askType, null);
        askPending.setAskType(askType);
        return askPending;
    }
}
