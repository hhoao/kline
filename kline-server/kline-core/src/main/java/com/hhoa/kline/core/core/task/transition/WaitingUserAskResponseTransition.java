package com.hhoa.kline.core.core.task.transition;

import com.hhoa.kline.core.core.shared.ClineAsk;
import com.hhoa.kline.core.core.task.TaskV2;
import com.hhoa.kline.core.core.task.event.AutoApprovalMaxReqReachedEvent;
import com.hhoa.kline.core.core.task.event.MaxMistakeLimitReachedEvent;
import com.hhoa.kline.core.core.task.event.TaskEvent;
import com.hhoa.kline.core.core.task.statemachine.SingleArcTransition;

public class WaitingUserAskResponseTransition implements SingleArcTransition<TaskV2, TaskEvent> {
    @Override
    public void transition(TaskV2 operand, TaskEvent event) {
        if (event instanceof MaxMistakeLimitReachedEvent maxMistakeLimitReachedEvent) {
            operand.getSayAskHandler()
                    .ask(ClineAsk.MISTAKE_LIMIT_REACHED, maxMistakeLimitReachedEvent.getMessage());
        } else if (event instanceof AutoApprovalMaxReqReachedEvent autoApprovalMaxReqReachedEvent) {
            operand.getSayAskHandler()
                    .ask(
                            ClineAsk.AUTO_APPROVAL_MAX_REQ_REACHED,
                            autoApprovalMaxReqReachedEvent.getMessage());
        }
    }
}
