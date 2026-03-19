package com.hhoa.kline.core.core.task.transition;

import com.hhoa.kline.core.core.task.TaskV2;
import com.hhoa.kline.core.core.task.event.ContextReadyEvent;
import com.hhoa.kline.core.core.task.event.MaxMistakeLimitReachedEvent;
import com.hhoa.kline.core.core.task.event.PrepareFailedEvent;
import com.hhoa.kline.core.core.task.event.TaskEvent;
import com.hhoa.kline.core.core.task.handler.PrepareContextResult;
import com.hhoa.kline.core.core.task.statemachine.SingleArcTransition;

public class PrepareContextTransition implements SingleArcTransition<TaskV2, TaskEvent> {

    @Override
    public void transition(TaskV2 operand, TaskEvent event) {
        try {
            PrepareContextResult result = operand.getContextPrepareHandler().doPrepareContext();
            switch (result) {
                case PrepareContextResult.Success ignored ->
                        operand.handle(new ContextReadyEvent(operand.getTaskId()));
                case PrepareContextResult.Failed ignored ->
                        operand.handle(
                                new PrepareFailedEvent(
                                        operand.getTaskId(),
                                        new Exception("Prepare context failed")));
                case PrepareContextResult.MaxMistakeLimitReached(String message) ->
                        operand.handle(
                                new MaxMistakeLimitReachedEvent(operand.getTaskId(), message));
                default -> {}
            }
        } catch (Throwable t) {
            operand.handle(new PrepareFailedEvent(operand.getTaskId(), t));
        }
    }
}
