package com.hhoa.kline.core.core.task.transition;

import com.hhoa.kline.core.core.shared.ClineAsk;
import com.hhoa.kline.core.core.shared.ClineAskResponse;
import com.hhoa.kline.core.core.task.AskResult;
import com.hhoa.kline.core.core.task.TaskState;
import com.hhoa.kline.core.core.task.TaskStatus;
import com.hhoa.kline.core.core.task.TaskV2;
import com.hhoa.kline.core.core.task.event.TaskEvent;
import com.hhoa.kline.core.core.task.event.UserRespondedEvent;
import com.hhoa.kline.core.core.task.statemachine.MultipleArcTransition;
import com.hhoa.kline.core.core.task.tools.types.PendingAskToken;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UserRespondedTransition
        implements MultipleArcTransition<TaskV2, TaskEvent, TaskStatus> {

    @Override
    public TaskStatus transition(TaskV2 operand, TaskEvent event) {
        UserRespondedEvent responded = (UserRespondedEvent) event;
        TaskState taskState = operand.getTaskState();
        AskResult askResult = responded.getAskResult();

        PendingAskToken pendingAskToken =
                taskState.getPendingAskTokens().get(responded.getPendingId());
        if (pendingAskToken == null) {
            taskState.getPendingUserResponses().offer(askResult);
            return TaskStatus.WAITING_USER_ASK_RESPONSE;
        } else {
            if (pendingAskToken.getAskType() == ClineAsk.MISTAKE_LIMIT_REACHED) {
                if (askResult.getResponse() == ClineAskResponse.YES_BUTTON_CLICKED) {
                    taskState.setConsecutiveMistakeCount(0);
                    return TaskStatus.PREPARE_CONTEXT;
                } else {
                    taskState.setConsecutiveMistakeCount(0);
                    return TaskStatus.TASK_COMPLETE;
                }
            }
            return TaskStatus.TASK_COMPLETE;
        }
    }
}
