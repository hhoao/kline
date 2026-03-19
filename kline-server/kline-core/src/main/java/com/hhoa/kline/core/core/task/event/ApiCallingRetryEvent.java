package com.hhoa.kline.core.core.task.event;

import lombok.Getter;

@Getter
public class ApiCallingRetryEvent extends TaskEvent {

    private final int delayMs;
    private final String errorMessage;

    public ApiCallingRetryEvent(String taskId, int delayMs, String errorMessage) {
        super(TaskEventType.API_CALLING_RETRY, taskId);
        this.delayMs = delayMs;
        this.errorMessage = errorMessage;
    }
}
