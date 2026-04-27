package com.hhoa.kline.core.core.tools.types;

import com.hhoa.kline.core.core.assistant.ToolUse;
import com.hhoa.kline.core.core.shared.ClineAsk;
import com.hhoa.kline.core.core.shared.ClineMessageFormat;
import lombok.Getter;

/** 表示一次"等待用户回复"的挂起点。事件循环收到 USER_RESPONDED 时凭 pendingId 恢复对应工具执行。 */
@Getter
public abstract class PendingAskToken {

    private final String pendingId;
    private final String taskId;
    private final ClineAsk askType;
    private final String text;
    private final ClineMessageFormat format;

    public PendingAskToken(
            String pendingId,
            String taskId,
            ClineAsk askType,
            String text,
            ClineMessageFormat format) {
        this.pendingId = pendingId;
        this.taskId = taskId;
        this.askType = askType;
        this.text = text;
        this.format = format;
    }

    @Getter
    public static class DefaultPendingAskToken extends PendingAskToken {
        public DefaultPendingAskToken(
                String pendingId,
                String taskId,
                ClineAsk askType,
                String text,
                ClineMessageFormat format) {
            super(pendingId, taskId, askType, text, format);
        }
    }

    @Getter
    public static class ToolUsePendingAskToken extends PendingAskToken {
        private final ToolUse toolUse;
        private final String toolDescription;

        public ToolUsePendingAskToken(
                String pendingId,
                String taskId,
                String toolDescription,
                ClineAsk askType,
                String text,
                ClineMessageFormat format,
                ToolUse toolUse) {
            super(pendingId, taskId, askType, text, format);
            this.toolUse = toolUse;
            this.toolDescription = toolDescription;
        }
    }
}
