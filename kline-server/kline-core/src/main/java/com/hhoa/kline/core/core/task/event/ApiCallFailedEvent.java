package com.hhoa.kline.core.core.task.event;

import lombok.Getter;

@Getter
public class ApiCallFailedEvent extends TaskEvent {

    private final Throwable cause;
    private final String message;

    public ApiCallFailedEvent(String taskId, Throwable cause, String message) {
        super(TaskEventType.API_CALLING_FAILED, taskId);
        this.cause = cause;
        this.message = message;
    }
}
