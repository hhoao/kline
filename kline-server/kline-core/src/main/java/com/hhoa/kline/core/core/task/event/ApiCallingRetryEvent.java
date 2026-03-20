package com.hhoa.kline.core.core.task.event;

import lombok.Getter;

@Getter
public class ApiCallingRetryEvent extends TaskEvent {

    public ApiCallingRetryEvent(String taskId) {
        super(TaskEventType.API_CALLING_RETRY, taskId);
    }
}
