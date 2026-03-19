package com.hhoa.kline.core.core.task.transition;

import com.hhoa.kline.core.core.task.TaskV2;
import com.hhoa.kline.core.core.task.event.ApiCallingRetryEvent;
import com.hhoa.kline.core.core.task.event.ContextReadyEvent;
import com.hhoa.kline.core.core.task.event.TaskEvent;
import com.hhoa.kline.core.core.task.statemachine.SingleArcTransition;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ApiRetryTransition implements SingleArcTransition<TaskV2, TaskEvent> {

    @Override
    public void transition(TaskV2 operand, TaskEvent event) {
        ApiCallingRetryEvent retryEvent = (ApiCallingRetryEvent) event;
        int delayMs = retryEvent.getDelayMs();

        CompletableFuture.delayedExecutor(delayMs, TimeUnit.MILLISECONDS)
                .execute(() -> operand.handle(new ContextReadyEvent(operand.getTaskId())));
    }
}
