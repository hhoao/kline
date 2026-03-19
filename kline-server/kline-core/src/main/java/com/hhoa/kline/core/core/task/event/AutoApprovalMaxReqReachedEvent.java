package com.hhoa.kline.core.core.task.event;

import lombok.Getter;

@Getter
public class AutoApprovalMaxReqReachedEvent extends TaskEvent {
    private final String message;

    public AutoApprovalMaxReqReachedEvent(String taskId, String message) {
        super(TaskEventType.AUTO_APPROVAL_MAX_REQ_REACHED, taskId);
        this.message = message;
    }
}
