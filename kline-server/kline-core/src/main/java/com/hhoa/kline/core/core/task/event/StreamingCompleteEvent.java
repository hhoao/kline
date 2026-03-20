package com.hhoa.kline.core.core.task.event;


public class StreamingCompleteEvent extends TaskEvent {

    public StreamingCompleteEvent(String taskId) {
        super(TaskEventType.STREAMING_COMPLETE, taskId);
    }
}
