package com.hhoa.kline.core.core.task.transition;

import com.hhoa.kline.core.core.shared.ClineAsk;
import com.hhoa.kline.core.core.shared.ClineMessageFormat;
import com.hhoa.kline.core.core.task.AskPending;
import com.hhoa.kline.core.core.task.TaskV2;
import com.hhoa.kline.core.core.task.event.AutoApprovalMaxReqReachedEvent;
import com.hhoa.kline.core.core.task.event.MaxMistakeLimitReachedEvent;
import com.hhoa.kline.core.core.task.event.TaskEvent;
import com.hhoa.kline.core.core.task.statemachine.SingleArcTransition;
import com.hhoa.kline.core.core.task.tools.types.PendingAskToken.DefaultPendingAskToken;

public class WaitingUserAskResponseTransition implements SingleArcTransition<TaskV2, TaskEvent> {
    @Override
    public void transition(TaskV2 operand, TaskEvent event) {
        if (event instanceof MaxMistakeLimitReachedEvent maxMistakeLimitReachedEvent) {
            registerAsk(
                    operand,
                    ClineAsk.MISTAKE_LIMIT_REACHED,
                    maxMistakeLimitReachedEvent.getMessage());
        } else if (event instanceof AutoApprovalMaxReqReachedEvent autoApprovalMaxReqReachedEvent) {
            registerAsk(
                    operand,
                    ClineAsk.AUTO_APPROVAL_MAX_REQ_REACHED,
                    autoApprovalMaxReqReachedEvent.getMessage());
        }
    }

    private void registerAsk(TaskV2 operand, ClineAsk askType, String message) {
        AskPending askPending = operand.getSayAskHandler().ask(askType, message);
        if (askPending.getPendingId() != null) {
            DefaultPendingAskToken token =
                    new DefaultPendingAskToken(
                            askPending.getPendingId(),
                            operand.getTaskId(),
                            askType,
                            message,
                            ClineMessageFormat.PLAIN);
            operand.getTaskState().getPendingAskTokens().put(askPending.getPendingId(), token);
        }
    }
}
