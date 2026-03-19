package com.hhoa.kline.core.core.task.event;

import lombok.Getter;

@Getter
public class ApiFailedEvent extends TaskEvent {

    private final Throwable cause;

    public ApiFailedEvent(String taskId, Throwable cause) {
        super(TaskEventType.API_CALLING_FAILED, taskId);
        this.cause = cause;
    }
}
