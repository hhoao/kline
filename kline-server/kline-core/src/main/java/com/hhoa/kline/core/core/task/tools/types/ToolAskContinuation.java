package com.hhoa.kline.core.core.task.tools.types;

import com.hhoa.kline.core.core.task.AskResult;

/**
 * 工具内 ask 的恢复入口。事件循环在收到 USER_RESPONDED（带 pendingId）时，根据 pendingId 找到对应 token， 再调用此接口传入
 * AskResult，得到该次工具执行的 UserContentBlock 结果并继续流程。
 */
public interface ToolAskContinuation {

    ToolExecuteResult resume(PendingAskToken token, AskResult askResult);
}
