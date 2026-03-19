package com.hhoa.kline.core.core.task.event;

/** 重试事件，用于 API 调用失败后重新进入 PREPARE_CONTEXT */
public class RetryEvent extends TaskEvent {

    public RetryEvent(String taskId) {
        super(TaskEventType.RETRY, taskId);
    }
}
