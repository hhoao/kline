package com.hhoa.kline.core.core.task.event;

public class NoRetryEvent extends TaskEvent {

    public NoRetryEvent(String taskId) {
        super(TaskEventType.NO_RETRY, taskId);
    }
}
