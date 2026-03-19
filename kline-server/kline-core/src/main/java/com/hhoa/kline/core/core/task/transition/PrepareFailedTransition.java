package com.hhoa.kline.core.core.task.transition;

import com.hhoa.kline.core.core.task.TaskV2;
import com.hhoa.kline.core.core.task.event.PrepareFailedEvent;
import com.hhoa.kline.core.core.task.event.TaskEvent;
import com.hhoa.kline.core.core.task.statemachine.SingleArcTransition;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PrepareFailedTransition implements SingleArcTransition<TaskV2, TaskEvent> {

    @Override
    public void transition(TaskV2 operand, TaskEvent event) {
        PrepareFailedEvent failedEvent = (PrepareFailedEvent) event;
        Throwable cause = failedEvent.getCause();
        log.error(
                "Task {} context preparation failed: {}",
                operand.getTaskId(),
                cause != null ? cause.getMessage() : "unknown error");
        operand.getAbortHandler().abort();
    }
}
