package com.hhoa.kline.core.core.task.event;

import lombok.Getter;

@Getter
public class PrepareFailedEvent extends TaskEvent {

    private final Throwable cause;

    public PrepareFailedEvent(String taskId, Throwable cause) {
        super(TaskEventType.PREPARE_FAILED, taskId);
        this.cause = cause;
    }
}
