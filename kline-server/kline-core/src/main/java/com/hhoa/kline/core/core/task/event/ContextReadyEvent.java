package com.hhoa.kline.core.core.task.event;

import lombok.Getter;

@Getter
public class ContextReadyEvent extends TaskEvent {
    public ContextReadyEvent(String taskId) {
        super(TaskEventType.CONTEXT_READY, taskId);
    }
}
