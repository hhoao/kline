package com.hhoa.kline.core.core.task.transition;

import com.hhoa.kline.core.core.shared.ClineAsk;
import com.hhoa.kline.core.core.shared.ClineAskResponse;
import com.hhoa.kline.core.core.shared.ClineSay;
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

    private static final PrepareContextTransition PREPARE_CONTEXT_TRANSITION =
            new PrepareContextTransition();

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
        }

        taskState.getPendingAskTokens().remove(responded.getPendingId());
        ClineAsk askType = pendingAskToken.getAskType();

        return switch (askType) {
            case MISTAKE_LIMIT_REACHED -> handleMistakeLimitReached(operand, askResult);
            case AUTO_APPROVAL_MAX_REQ_REACHED -> handleAutoApprovalMaxReq(operand, askResult);
            case RESUME_TASK, RESUME_COMPLETED_TASK -> handleResumeTask(operand, askResult);
            default -> {
                log.warn("Unhandled ask type in WAITING_USER_ASK_RESPONSE: {}", askType);
                operand.getAbortHandler().abort();
                yield TaskStatus.TASK_COMPLETE;
            }
        };
    }

    private TaskStatus handleMistakeLimitReached(TaskV2 operand, AskResult askResult) {
        operand.getTaskState().setConsecutiveMistakeCount(0);
        if (askResult.getResponse() == ClineAskResponse.YES_BUTTON_CLICKED) {
            PREPARE_CONTEXT_TRANSITION.transition(operand, null);
            return TaskStatus.PREPARE_CONTEXT;
        }
        operand.getAbortHandler().abort();
        return TaskStatus.TASK_COMPLETE;
    }

    private TaskStatus handleAutoApprovalMaxReq(TaskV2 operand, AskResult askResult) {
        if (askResult.getResponse() == ClineAskResponse.YES_BUTTON_CLICKED) {
            PREPARE_CONTEXT_TRANSITION.transition(operand, null);
            return TaskStatus.PREPARE_CONTEXT;
        }
        operand.getAbortHandler().abort();
        return TaskStatus.TASK_COMPLETE;
    }

    private TaskStatus handleResumeTask(TaskV2 operand, AskResult askResult) {
        if (askResult.getResponse() == ClineAskResponse.MESSAGE_RESPONSE) {
            operand.getSayAskHandler()
                    .say(
                            ClineSay.USER_FEEDBACK,
                            askResult.getText(),
                            askResult.getImages(),
                            askResult.getFiles(),
                            null);
        }

        if (askResult.getResponse() != ClineAskResponse.NO_BUTTON_CLICKED) {
            PREPARE_CONTEXT_TRANSITION.transition(operand, null);
            return TaskStatus.PREPARE_CONTEXT;
        }
        operand.getAbortHandler().abort();
        return TaskStatus.TASK_COMPLETE;
    }
}
