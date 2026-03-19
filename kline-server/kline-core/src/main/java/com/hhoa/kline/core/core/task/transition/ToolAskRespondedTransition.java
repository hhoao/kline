package com.hhoa.kline.core.core.task.transition;

import com.hhoa.kline.core.core.task.ApiRequestResult;
import com.hhoa.kline.core.core.task.TaskV2;
import com.hhoa.kline.core.core.task.event.ApiCompletedEvent;
import com.hhoa.kline.core.core.task.event.TaskEvent;
import com.hhoa.kline.core.core.task.event.UserRespondedEvent;
import com.hhoa.kline.core.core.task.statemachine.SingleArcTransition;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ToolAskRespondedTransition implements SingleArcTransition<TaskV2, TaskEvent> {

    @Override
    public void transition(TaskV2 operand, TaskEvent event) {
        UserRespondedEvent responded = (UserRespondedEvent) event;

        boolean allDone =
                operand.getMessagePresenterHandler()
                        .continueAfterToolAskResolved(
                                responded.getPendingId(), responded.getAskResult());

        if (!allDone) {
            log.debug("Another PendingAsk encountered, staying in WAITING_API_CALLING_COMPLETED");
            return;
        }

        ApiRequestResult apiResult = operand.getTaskState().getApiRequestResult();

        operand.handle(new ApiCompletedEvent(operand.getTaskId(), apiResult));
    }
}
