package com.hhoa.kline.core.core.task.event;

import lombok.Getter;

@Getter
public class MaxMistakeLimitReachedEvent extends TaskEvent {
    private final String message;

    public MaxMistakeLimitReachedEvent(String taskId, String message) {
        super(TaskEventType.MAX_MISTAKE_LIMIT_REACHED, taskId);
        this.message = message;
    }
}
