package com.hhoa.kline.core.core.task.event;

import com.hhoa.kline.core.core.shared.ClineAsk;
import com.hhoa.kline.core.core.shared.ClineMessageFormat;
import lombok.Data;

/**
 * AskUserEvent
 *
 * @author xianxing
 * @since 2026/3/20
 */
@Data
public class AskUserEvent extends TaskEvent {
    private final ClineAsk askType;
    private final String text;
    private final ClineMessageFormat format;

    public AskUserEvent(String taskId, ClineAsk askType, String text, ClineMessageFormat format) {
        super(TaskEventType.ASK_USER, taskId);
        this.askType = askType;
        this.text = text;
        this.format = format;
    }

    public AskUserEvent(String taskId, ClineAsk askType, String message) {
        this(taskId, askType, message, ClineMessageFormat.PLAIN);
    }
}
