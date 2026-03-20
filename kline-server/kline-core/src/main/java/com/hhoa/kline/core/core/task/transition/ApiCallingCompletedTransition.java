package com.hhoa.kline.core.core.task.transition;

import com.hhoa.kline.core.core.shared.ClineAsk;
import com.hhoa.kline.core.core.task.ClineRequestResult;
import com.hhoa.kline.core.core.task.TaskV2;
import com.hhoa.kline.core.core.task.event.AbortTaskEvent;
import com.hhoa.kline.core.core.task.event.ApiCallingRetryEvent;
import com.hhoa.kline.core.core.task.event.ApiCompletedEvent;
import com.hhoa.kline.core.core.task.event.AskUserEvent;
import com.hhoa.kline.core.core.task.event.ContinueNextTurnEvent;
import com.hhoa.kline.core.core.task.event.TaskCompleteEvent;
import com.hhoa.kline.core.core.task.event.TaskEvent;
import com.hhoa.kline.core.core.task.statemachine.SingleArcTransition;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ApiCallingCompletedTransition implements SingleArcTransition<TaskV2, TaskEvent> {

    @Override
    public void transition(TaskV2 operand, TaskEvent event) {
        ApiCompletedEvent apiCompletedEvent = (ApiCompletedEvent) event;

        ClineRequestResult processResult =
                operand.getApiCallHandler()
                        .processAssistantResponse(
                                apiCompletedEvent.getApiRequestResult(),
                                operand.getCurrentProviderInfo());

        log.debug("Task {} API completed with result: {}", operand.getTaskId(), processResult);

        if (processResult instanceof ClineRequestResult.Abort) {
            operand.handle(new AbortTaskEvent(operand.getTaskId()));
        } else if (processResult
                instanceof ClineRequestResult.Failed(String message, Throwable throwable)) {
            boolean tryRetry = operand.getApiCallHandler().tryRetryAsk(message);
            if (!tryRetry) {
                operand.handle(new ApiCallingRetryEvent(operand.getTaskId()));
            } else {
                operand.handle(
                        new AskUserEvent(
                                operand.getTaskId(),
                                ClineAsk.PROCESS_ASSISTANT_RESPONSE_FAILED,
                                message));
            }
        } else if (processResult instanceof ClineRequestResult.DidNotToolUse) {
            operand.handle(new TaskCompleteEvent(operand.getTaskId()));
        } else if (processResult instanceof ClineRequestResult.DidToolUse) {
            operand.getTaskState()
                    .setCurrentUserContent(operand.getTaskState().getNextUserMessageContent());
            operand.getTaskState().setCurrentIncludeFileDetails(false);
            operand.handle(new ContinueNextTurnEvent(operand.getTaskId(), null, null, null));
        }
    }
}
