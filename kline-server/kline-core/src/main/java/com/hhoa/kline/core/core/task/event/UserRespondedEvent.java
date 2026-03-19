package com.hhoa.kline.core.core.task.event;

import com.hhoa.kline.core.core.task.AskResult;
import lombok.Getter;

@Getter
public class UserRespondedEvent extends TaskEvent {
    private final String pendingId;
    private final AskResult askResult;

    public UserRespondedEvent(String taskId, String pendingId, AskResult askResult) {
        super(TaskEventType.USER_RESPONDED, taskId);
        this.pendingId = pendingId;
        this.askResult = askResult;
    }
}
