package com.hhoa.kline.core.core.task.transition;

import com.hhoa.kline.core.core.shared.ClineAsk;
import com.hhoa.kline.core.core.shared.ClineAskResponse;
import com.hhoa.kline.core.core.task.ApiRequestResult;
import com.hhoa.kline.core.core.task.AskResult;
import com.hhoa.kline.core.core.task.TaskV2;
import com.hhoa.kline.core.core.task.event.ApiCallFailedEvent;
import com.hhoa.kline.core.core.task.event.ApiCallingRetryEvent;
import com.hhoa.kline.core.core.task.event.ApiCompletedEvent;
import com.hhoa.kline.core.core.task.event.TaskEvent;
import com.hhoa.kline.core.core.task.event.UserRespondedEvent;
import com.hhoa.kline.core.core.task.statemachine.SingleArcTransition;
import com.hhoa.kline.core.core.task.tools.types.PendingAskToken;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ApiCallingToolAskRespondedTransition
        implements SingleArcTransition<TaskV2, TaskEvent> {

    @Override
    public void transition(TaskV2 operand, TaskEvent event) {
        UserRespondedEvent responded = (UserRespondedEvent) event;

        PendingAskToken pendingAskToken =
                operand.getTaskState().getPendingAskTokens().get(responded.getPendingId());
        ClineAsk askType = pendingAskToken.getAskType();
        AskResult askResult = responded.getAskResult();
        if (askType == ClineAsk.API_REQ_FAILED) {
            if (askResult.getResponse() == ClineAskResponse.YES_BUTTON_CLICKED) {
                operand.handle(new ApiCallingRetryEvent(operand.getTaskId()));
            } else {
                operand.handle(
                        new ApiCallFailedEvent(
                                operand.getTaskId(),
                                new Exception("User responded with no button clicked"),
                                "User responded with no button clicked"));
            }
        } else {
            boolean allDone =
                    operand.getMessagePresenterHandler()
                            .continueAfterToolAskResolved(
                                    responded.getPendingId(), responded.getAskResult());

            if (!allDone) {
                log.debug(
                        "Another PendingAsk encountered, staying in WAITING_API_CALLING_COMPLETED");
                return;
            }

            ApiRequestResult apiResult = operand.getTaskState().getApiRequestResult();

            operand.handle(new ApiCompletedEvent(operand.getTaskId(), apiResult));
        }
    }
}
