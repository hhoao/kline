package com.hhoa.kline.core.core.task.event;

public class ResumeTaskEvent extends TaskEvent {

    public ResumeTaskEvent(String taskId) {
        super(TaskEventType.RESUME_TASK, taskId);
    }
}
