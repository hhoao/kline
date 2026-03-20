package com.hhoa.kline.core.core.task.event;

public class RetryPrepareContextEvent extends TaskEvent {

    public RetryPrepareContextEvent(String taskId) {
        super(TaskEventType.RETRY_PREPARE_CONTEXT, taskId);
    }
}
