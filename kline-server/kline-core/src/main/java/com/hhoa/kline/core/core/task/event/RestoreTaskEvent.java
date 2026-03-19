package com.hhoa.kline.core.core.task.event;

public class RestoreTaskEvent extends TaskEvent {

    public RestoreTaskEvent(String taskId) {
        super(TaskEventType.RESTORE_TASK, taskId);
    }
}
