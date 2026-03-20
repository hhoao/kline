package com.hhoa.kline.core.core.task.transition;

import com.hhoa.kline.core.core.shared.ClineAsk;
import com.hhoa.kline.core.core.shared.ClineMessageFormat;
import com.hhoa.kline.core.core.task.AskPending;
import com.hhoa.kline.core.core.task.TaskV2;
import com.hhoa.kline.core.core.task.event.AskUserEvent;
import com.hhoa.kline.core.core.task.event.TaskEvent;
import com.hhoa.kline.core.core.task.statemachine.SingleArcTransition;
import com.hhoa.kline.core.core.task.tools.types.PendingAskToken;

public class AskUserTransition implements SingleArcTransition<TaskV2, TaskEvent> {

    @Override
    public void transition(TaskV2 operand, TaskEvent event) {

        AskUserEvent askUserEvent = (AskUserEvent) event;
        registerAsk(operand, askUserEvent.getAskType(), askUserEvent.getText());
    }

    private void registerAsk(TaskV2 operand, ClineAsk askType, String message) {
        AskPending askPending = operand.getSayAskHandler().ask(askType, message);
        if (askPending.getPendingId() != null) {
            PendingAskToken.DefaultPendingAskToken token =
                    new PendingAskToken.DefaultPendingAskToken(
                            askPending.getPendingId(),
                            operand.getTaskId(),
                            askType,
                            message,
                            ClineMessageFormat.PLAIN);
            operand.getTaskState().getPendingAskTokens().put(askPending.getPendingId(), token);
        }
    }
}
