package com.hhoa.kline.core.core.task.event;

public class AbortTaskEvent extends TaskEvent {

    public AbortTaskEvent(String taskId) {
        super(TaskEventType.ABORT, taskId);
    }
}
