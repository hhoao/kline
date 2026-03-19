package com.hhoa.kline.core.core.task.event;

import lombok.Getter;

@Getter
public class TaskCompletedEvent extends TaskEvent {

    public TaskCompletedEvent(String taskId) {
        super(TaskEventType.TASK_COMPLETE, taskId);
    }
}
