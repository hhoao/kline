package com.hhoa.kline.core.core.task.event;

import lombok.Getter;

@Getter
public class TaskCompleteEvent extends TaskEvent {

    public TaskCompleteEvent(String taskId) {
        super(TaskEventType.TASK_COMPLETE, taskId);
    }
}
