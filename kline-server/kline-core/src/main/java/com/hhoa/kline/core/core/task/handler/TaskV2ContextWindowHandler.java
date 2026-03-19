package com.hhoa.kline.core.core.task.handler;

import com.hhoa.kline.core.core.assistant.MessageParam;
import com.hhoa.kline.core.core.context.management.ContextManager;
import com.hhoa.kline.core.core.context.management.KeepStrategy;
import com.hhoa.kline.core.core.shared.ClineSay;
import com.hhoa.kline.core.core.task.MessageStateHandler;
import com.hhoa.kline.core.core.task.TaskState;
import java.util.List;

public final class TaskV2ContextWindowHandler {

    private final ContextManager contextManager;
    private final MessageStateHandler messageStateHandler;
    private final TaskState taskState;
    private final TaskV2SayAskHandler sayAskHandler;

    public TaskV2ContextWindowHandler(
            ContextManager contextManager,
            MessageStateHandler messageStateHandler,
            TaskState taskState,
            TaskV2SayAskHandler sayAskHandler) {
        this.contextManager = contextManager;
        this.messageStateHandler = messageStateHandler;
        this.taskState = taskState;
        this.sayAskHandler = sayAskHandler;
    }

    public void handleContextWindowExceededError() {
        List<MessageParam> apiConversationHistory = messageStateHandler.getApiConversationHistory();

        if (apiConversationHistory.size() > 4) {
            int[] currentRange = taskState.getConversationHistoryDeletedRange();
            int[] newRange =
                    contextManager.getNextTruncationRange(
                            apiConversationHistory, currentRange, KeepStrategy.QUARTER);

            taskState.setConversationHistoryDeletedRange(newRange);

            messageStateHandler.saveClineMessagesAndUpdateHistory();

            contextManager.triggerApplyStandardContextTruncationNoticeChange(
                    System.currentTimeMillis(), apiConversationHistory);

            sayAskHandler.say(
                    ClineSay.TEXT,
                    String.format(
                            "Context window exceeded. Truncated conversation history (removed messages %d-%d).",
                            newRange[0], newRange[1]),
                    null,
                    null,
                    null);
        } else {
            sayAskHandler.say(
                    ClineSay.ERROR,
                    "Context window exceeded, but conversation history is too short to truncate. "
                            + "Please provide a shorter initial task or start a new conversation.",
                    null,
                    null,
                    null);
        }

        messageStateHandler.saveClineMessagesAndUpdateHistory();

        taskState.setDidAutomaticallyRetryFailedApiRequest(true);
    }
}
