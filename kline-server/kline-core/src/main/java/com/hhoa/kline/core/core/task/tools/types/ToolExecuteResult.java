package com.hhoa.kline.core.core.task.tools.types;

import com.hhoa.kline.core.core.assistant.UserContentBlock;
import java.util.List;

/** 工具执行结果。事件驱动下若工具内需要 ask，则返回 PendingAsk 而非阻塞。 */
public interface ToolExecuteResult {

    record Immediate(List<UserContentBlock> blocks) implements ToolExecuteResult {}

    record PendingAsk(PendingAskToken.ToolUsePendingAskToken token) implements ToolExecuteResult {}

    record Partial() implements ToolExecuteResult {}
}
