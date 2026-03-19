package com.hhoa.kline.core.core.task.event;

public class AskUserForResetRetryTimesEvent extends TaskEvent {

    public AskUserForResetRetryTimesEvent(String taskId) {
        super(TaskEventType.ASK_USER_FOR_RESET_RETRY_TIMES, taskId);
    }
}
