package com.hhoa.kline.core.core.task.event;

import lombok.Data;

@Data
public class PrepareContextEvent extends TaskEvent {

    public PrepareContextEvent(String taskId) {
        super(TaskEventType.PREPARE_CONTEXT, taskId);
    }
}
