package com.hhoa.kline.core.core.task.event;

import java.util.List;
import lombok.Getter;

@Getter
public class StartTaskEvent extends TaskEvent {

    private final String taskText;
    private final List<String> images;
    private final List<String> files;

    public StartTaskEvent(String taskId, String taskText, List<String> images, List<String> files) {
        super(TaskEventType.START_TASK, taskId);
        this.taskText = taskText;
        this.images = images;
        this.files = files;
    }
}
