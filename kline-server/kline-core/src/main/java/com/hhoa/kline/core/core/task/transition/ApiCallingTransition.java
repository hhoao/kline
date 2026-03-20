package com.hhoa.kline.core.core.task.transition;

import com.hhoa.kline.core.core.shared.ClineAsk;
import com.hhoa.kline.core.core.task.ApiRequestResult;
import com.hhoa.kline.core.core.task.ExistState;
import com.hhoa.kline.core.core.task.TaskV2;
import com.hhoa.kline.core.core.task.event.AbortTaskEvent;
import com.hhoa.kline.core.core.task.event.ApiCallFailedEvent;
import com.hhoa.kline.core.core.task.event.ApiCallingRetryEvent;
import com.hhoa.kline.core.core.task.event.AskUserEvent;
import com.hhoa.kline.core.core.task.event.RetryPrepareContextEvent;
import com.hhoa.kline.core.core.task.event.StreamingCompleteEvent;
import com.hhoa.kline.core.core.task.event.TaskEvent;
import com.hhoa.kline.core.core.task.statemachine.SingleArcTransition;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ApiCallingTransition implements SingleArcTransition<TaskV2, TaskEvent> {
    @Override
    public void transition(TaskV2 operand, TaskEvent event) {
        CompletableFuture.runAsync(
                        () -> {
                            ApiRequestResult result = operand.getApiCallHandler().doCallApi();
                            operand.getTaskState().setApiRequestResult(result);

                            if (result.getExistState() instanceof ExistState.Abort) {
                                operand.handle(new AbortTaskEvent(operand.getTaskId()));
                            } else if (result.getExistState()
                                    instanceof ExistState.ContextWindowExceeded) {
                                operand.handle(new RetryPrepareContextEvent(operand.getTaskId()));
                            } else if (result.getExistState()
                                    instanceof
                                    ExistState.Failed(String message, Throwable throwable)) {
                                boolean tryRetry = operand.getApiCallHandler().tryRetryAsk(message);
                                if (!tryRetry) {
                                    operand.handle(new ApiCallingRetryEvent(operand.getTaskId()));
                                } else {
                                    operand.handle(
                                            new AskUserEvent(
                                                    operand.getTaskId(),
                                                    ClineAsk.API_REQ_FAILED,
                                                    message));
                                }
                            } else if (result.getExistState() instanceof ExistState.Success) {
                                operand.handle(new StreamingCompleteEvent(operand.getTaskId()));
                            }
                        })
                .exceptionally(
                        ex -> {
                            log.error("API call failed with unexpected exception", ex);
                            operand.handle(
                                    new ApiCallFailedEvent(
                                            operand.getTaskId(), ex, ex.getMessage()));
                            return null;
                        });
    }
}
