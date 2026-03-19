package com.hhoa.kline.core.core.task.transition;

import com.hhoa.kline.core.core.task.ClineRequestResult;
import com.hhoa.kline.core.core.task.TaskV2;
import com.hhoa.kline.core.core.task.event.AbortTaskEvent;
import com.hhoa.kline.core.core.task.event.ApiCompletedEvent;
import com.hhoa.kline.core.core.task.event.ApiFailedEvent;
import com.hhoa.kline.core.core.task.event.ContinueNextTurnEvent;
import com.hhoa.kline.core.core.task.event.TaskCompletedEvent;
import com.hhoa.kline.core.core.task.event.TaskEvent;
import com.hhoa.kline.core.core.task.statemachine.SingleArcTransition;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ApiCompletedTransition implements SingleArcTransition<TaskV2, TaskEvent> {

    @Override
    public void transition(TaskV2 operand, TaskEvent event) {
        ApiCompletedEvent apiCompletedEvent = (ApiCompletedEvent) event;

        ClineRequestResult processResult =
                operand.getApiCallHandler()
                        .processAssistantResponse(
                                apiCompletedEvent.getApiRequestResult(),
                                operand.getCurrentProviderInfo());

        log.debug("Task {} API completed with result: {}", operand.getTaskId(), processResult);

        if (processResult == ClineRequestResult.ABORT) {
            operand.handle(new AbortTaskEvent(operand.getTaskId()));
        } else if (processResult == ClineRequestResult.FAILED) {
            operand.handle(new ApiFailedEvent(operand.getTaskId(), null));
        } else if (processResult == ClineRequestResult.DID_NOT_TOOL_USE) {
            operand.handle(new TaskCompletedEvent(operand.getTaskId()));
        } else if (processResult == ClineRequestResult.DID_TOOL_USE) {
            operand.getTaskState()
                    .setCurrentUserContent(operand.getTaskState().getNextUserMessageContent());
            operand.getTaskState().setCurrentIncludeFileDetails(false);
            operand.handle(new ContinueNextTurnEvent(operand.getTaskId(), null, null, null));
        }
    }
}
