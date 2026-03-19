package com.hhoa.kline.core.core.task.event;

import java.util.List;
import lombok.Getter;

@Getter
public class ContinueNextTurnEvent extends TaskEvent {
    private final String text;
    private final List<String> images;
    private final List<String> files;

    public ContinueNextTurnEvent(
            String taskId, String text, List<String> images, List<String> files) {
        super(TaskEventType.CONTINUE_NEXT_TURN, taskId);
        this.text = text;
        this.images = images;
        this.files = files;
    }
}
