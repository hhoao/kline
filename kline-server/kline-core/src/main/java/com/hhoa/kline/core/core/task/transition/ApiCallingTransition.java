package com.hhoa.kline.core.core.task.transition;

import com.hhoa.kline.core.core.shared.ClineAsk;
import com.hhoa.kline.core.core.shared.ClineMessageFormat;
import com.hhoa.kline.core.core.task.ApiRequestResult;
import com.hhoa.kline.core.core.task.AskPending;
import com.hhoa.kline.core.core.task.ExistState;
import com.hhoa.kline.core.core.task.TaskV2;
import com.hhoa.kline.core.core.task.event.AbortTaskEvent;
import com.hhoa.kline.core.core.task.event.ApiCallingRetryEvent;
import com.hhoa.kline.core.core.task.event.RetryPrepareContextEvent;
import com.hhoa.kline.core.core.task.event.TaskEvent;
import com.hhoa.kline.core.core.task.statemachine.SingleArcTransition;
import com.hhoa.kline.core.core.task.tools.types.PendingAskToken.DefaultPendingAskToken;
import java.util.concurrent.CompletableFuture;

public class ApiCallingTransition implements SingleArcTransition<TaskV2, TaskEvent> {
    @Override
    public void transition(TaskV2 operand, TaskEvent event) {
        CompletableFuture.runAsync(
                () -> {
                    ApiRequestResult result = operand.getApiCallHandler().doCallApi();
                    if (result.getExistState() instanceof ExistState.Abort) {
                        operand.handle(new AbortTaskEvent(operand.getTaskId()));
                    } else if (result.getExistState() instanceof ExistState.ContextWindowExceeded) {
                        operand.handle(new RetryPrepareContextEvent(operand.getTaskId()));
                    } else if (result.getExistState()
                            instanceof ExistState.Failed(String message, Throwable throwable)) {
                        AskPending tryRetryAsk = operand.getApiCallHandler().tryRetryAsk(message);
                        if (tryRetryAsk == null) {
                            operand.handle(new ApiCallingRetryEvent(operand.getTaskId()));
                        } else {
                            DefaultPendingAskToken pendingAskToken =
                                    new DefaultPendingAskToken(
                                            tryRetryAsk.getPendingId(),
                                            operand.getTaskId(),
                                            ClineAsk.API_REQ_FAILED,
                                            message,
                                            ClineMessageFormat.PLAIN);
                            operand.getTaskState()
                                    .getPendingAskTokens()
                                    .put(tryRetryAsk.getPendingId(), pendingAskToken);
                        }
                    }
                    operand.getTaskState().setApiRequestResult(result);
                });
    }
}
