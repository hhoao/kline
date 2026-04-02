package com.hhoa.kline.core.core.task.transition;

import com.hhoa.kline.core.core.shared.ClineAsk;
import com.hhoa.kline.core.core.task.TaskV2;
import com.hhoa.kline.core.core.task.event.AskUserEvent;
import com.hhoa.kline.core.core.task.event.ContextReadyEvent;
import com.hhoa.kline.core.core.task.event.PrepareFailedEvent;
import com.hhoa.kline.core.core.task.event.TaskCompleteEvent;
import com.hhoa.kline.core.core.task.event.TaskEvent;
import com.hhoa.kline.core.core.task.handler.PrepareContextResult;
import com.hhoa.kline.core.core.task.statemachine.SingleArcTransition;
import java.util.concurrent.CompletableFuture;

public class PrepareContextTransition implements SingleArcTransition<TaskV2, TaskEvent> {

    @Override
    public void transition(TaskV2 operand, TaskEvent event) {
        CompletableFuture.runAsync(
                () -> {
                    try {
                        PrepareContextResult result =
                                operand.getContextPrepareHandler().doPrepareContext();

                        switch (result) {
                            case PrepareContextResult.Success ignored ->
                                    operand.handle(new ContextReadyEvent(operand.getTaskId()));
                            case PrepareContextResult.Failed ignored ->
                                    operand.handle(
                                            new PrepareFailedEvent(
                                                    operand.getTaskId(),
                                                    new Exception("Prepare context failed")));
                            case PrepareContextResult.MaxMistakeLimitReached(String message) -> {
                                operand.handle(
                                        new AskUserEvent(
                                                operand.getTaskId(),
                                                ClineAsk.MISTAKE_LIMIT_REACHED,
                                                message));
                            }
                            case PrepareContextResult.AutoApprovalMaxReqReached(String message) -> {
                                operand.handle(
                                        new AskUserEvent(
                                                operand.getTaskId(),
                                                ClineAsk.AUTO_APPROVAL_MAX_REQ_REACHED,
                                                message));
                            }
                            case PrepareContextResult.EndLoop ignored ->
                                    operand.handle(new TaskCompleteEvent(operand.getTaskId()));
                            default -> {}
                        }
                    } catch (Throwable t) {
                        operand.handle(new PrepareFailedEvent(operand.getTaskId(), t));
                    }
                });
    }
}
