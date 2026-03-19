package com.hhoa.kline.core.core.task.event;

public class EndTaskEvent extends TaskEvent {

    public EndTaskEvent(String taskId) {
        super(TaskEventType.TASK_COMPLETE, taskId);
    }
}
