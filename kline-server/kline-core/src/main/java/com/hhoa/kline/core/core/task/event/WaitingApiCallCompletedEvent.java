package com.hhoa.kline.core.core.task.event;

import lombok.Getter;

@Getter
public class WaitingApiCallCompletedEvent extends TaskEvent {
    public WaitingApiCallCompletedEvent(String taskId) {
        super(TaskEventType.WAITING_API_CALL_COMPLETED, taskId);
    }
}
