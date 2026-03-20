package com.hhoa.kline.core.core.task.transition;

import com.hhoa.kline.core.core.shared.ClineMessageFormat;
import com.hhoa.kline.core.core.task.AskPending;
import com.hhoa.kline.core.core.task.TaskV2;
import com.hhoa.kline.core.core.task.event.TaskEvent;
import com.hhoa.kline.core.core.task.statemachine.SingleArcTransition;
import com.hhoa.kline.core.core.task.tools.types.PendingAskToken.DefaultPendingAskToken;

public class ResumeTaskTransition implements SingleArcTransition<TaskV2, TaskEvent> {

    @Override
    public void transition(TaskV2 operand, TaskEvent event) {
        AskPending askPending = operand.getResumeHandler().resumeTaskFromHistory();
        if (askPending != null && askPending.getPendingId() != null) {
            DefaultPendingAskToken token =
                    new DefaultPendingAskToken(
                            askPending.getPendingId(),
                            operand.getTaskId(),
                            askPending.getAskType(),
                            null,
                            ClineMessageFormat.PLAIN);
            operand.getTaskState().getPendingAskTokens().put(askPending.getPendingId(), token);
        }
    }
}
