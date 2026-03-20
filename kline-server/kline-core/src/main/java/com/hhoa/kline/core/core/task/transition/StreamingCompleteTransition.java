package com.hhoa.kline.core.core.task.transition;

import com.hhoa.kline.core.core.task.ApiRequestResult;
import com.hhoa.kline.core.core.task.TaskV2;
import com.hhoa.kline.core.core.task.event.ApiCompletedEvent;
import com.hhoa.kline.core.core.task.event.TaskEvent;
import com.hhoa.kline.core.core.task.statemachine.SingleArcTransition;
import lombok.extern.slf4j.Slf4j;

/**
 * CALLING_API 自循环：流式接收完成后，在事件循环线程上检查是否有待处理的 tool ask。
 *
 * <p>由于 eventQueue 是 FIFO，此事件一定在流式期间产生的所有 ASK_USER 事件之后被处理， 因此此刻检查 pendingAskTokens 是线程安全且时序正确的。
 */
@Slf4j
public class StreamingCompleteTransition implements SingleArcTransition<TaskV2, TaskEvent> {

    @Override
    public void transition(TaskV2 operand, TaskEvent event) {
        if (operand.getTaskState().getPendingAskTokens().isEmpty()) {
            ApiRequestResult result = operand.getTaskState().getApiRequestResult();
            operand.handle(new ApiCompletedEvent(operand.getTaskId(), result));
        } else {
            log.debug(
                    "Task {} streaming complete but {} tool asks pending, waiting for user response",
                    operand.getTaskId(),
                    operand.getTaskState().getPendingAskTokens().size());
        }
    }
}
