package com.hhoa.kline.core.core.task;

import com.hhoa.kline.core.core.task.event.TaskEvent;
import com.hhoa.kline.core.core.task.event.TaskEventType;
import com.hhoa.kline.core.core.task.statemachine.StateMachineFactory;
import com.hhoa.kline.core.core.task.transition.AbortTransition;
import com.hhoa.kline.core.core.task.transition.ApiCallingCompletedTransition;
import com.hhoa.kline.core.core.task.transition.ApiCallingToolAskRespondedTransition;
import com.hhoa.kline.core.core.task.transition.ApiCallingTransition;
import com.hhoa.kline.core.core.task.transition.AskUserTransition;
import com.hhoa.kline.core.core.task.transition.NoopTransition;
import com.hhoa.kline.core.core.task.transition.PrepareContextTransition;
import com.hhoa.kline.core.core.task.transition.PrepareFailedTransition;
import com.hhoa.kline.core.core.task.transition.ResumeTaskTransition;
import com.hhoa.kline.core.core.task.transition.StartTaskTransition;
import com.hhoa.kline.core.core.task.transition.StreamingCompleteTransition;
import com.hhoa.kline.core.core.task.transition.TaskCompleteTransition;
import com.hhoa.kline.core.core.task.transition.UserRespondedTransition;

final class TaskV2StateMachineTopology {

    private TaskV2StateMachineTopology() {}

    static StateMachineFactory<TaskV2, TaskStatus, TaskEventType, TaskEvent> installedFactory() {
        StateMachineFactory<TaskV2, TaskStatus, TaskEventType, TaskEvent> f =
                new StateMachineFactory<>(TaskStatus.NEW);
        f = fromNew(f);
        f = fromStartTask(f);
        f = fromPrepareContext(f);
        f = fromCallingApi(f);
        f = fromApiCompleted(f);
        f = fromWaitingUserAskResponse(f);
        f = fromAbort(f);
        f = fromTaskComplete(f);
        return f.installTopology();
    }

    private static StateMachineFactory<TaskV2, TaskStatus, TaskEventType, TaskEvent> fromNew(
            StateMachineFactory<TaskV2, TaskStatus, TaskEventType, TaskEvent> f) {
        return f.addTransition(
                        TaskStatus.NEW,
                        TaskStatus.START_TASK,
                        TaskEventType.START_TASK,
                        new StartTaskTransition())
                .addTransition(
                        TaskStatus.NEW,
                        TaskStatus.WAITING_USER_ASK_RESPONSE,
                        TaskEventType.RESUME_TASK,
                        new ResumeTaskTransition());
    }

    private static StateMachineFactory<TaskV2, TaskStatus, TaskEventType, TaskEvent> fromStartTask(
            StateMachineFactory<TaskV2, TaskStatus, TaskEventType, TaskEvent> f) {
        return f.addTransition(
                TaskStatus.START_TASK,
                TaskStatus.PREPARE_CONTEXT,
                TaskEventType.PREPARE_CONTEXT,
                new PrepareContextTransition());
    }

    private static StateMachineFactory<TaskV2, TaskStatus, TaskEventType, TaskEvent>
            fromPrepareContext(
                    StateMachineFactory<TaskV2, TaskStatus, TaskEventType, TaskEvent> f) {
        return f.addTransition(
                        TaskStatus.PREPARE_CONTEXT,
                        TaskStatus.CALLING_API,
                        TaskEventType.CONTEXT_READY,
                        new ApiCallingTransition())
                .addTransition(
                        TaskStatus.PREPARE_CONTEXT,
                        TaskStatus.WAITING_USER_ASK_RESPONSE,
                        TaskEventType.ASK_USER,
                        new AskUserTransition())
                .addTransition(
                        TaskStatus.PREPARE_CONTEXT,
                        TaskStatus.ABORT,
                        TaskEventType.PREPARE_FAILED,
                        new PrepareFailedTransition())
                .addTransition(
                        TaskStatus.PREPARE_CONTEXT,
                        TaskStatus.ABORT,
                        TaskEventType.ABORT,
                        new AbortTransition());
    }

    private static StateMachineFactory<TaskV2, TaskStatus, TaskEventType, TaskEvent> fromCallingApi(
            StateMachineFactory<TaskV2, TaskStatus, TaskEventType, TaskEvent> f) {
        return f.addTransition(
                        TaskStatus.CALLING_API,
                        TaskStatus.PREPARE_CONTEXT,
                        TaskEventType.RETRY_PREPARE_CONTEXT,
                        new PrepareContextTransition())
                .addTransition(
                        TaskStatus.CALLING_API,
                        TaskStatus.ABORT,
                        TaskEventType.ABORT,
                        new AbortTransition())
                .addTransition(
                        TaskStatus.CALLING_API,
                        TaskStatus.API_COMPLETED,
                        TaskEventType.API_COMPLETED,
                        new ApiCallingCompletedTransition())
                .addTransition(
                        TaskStatus.CALLING_API,
                        TaskStatus.CALLING_API,
                        TaskEventType.USER_RESPONDED,
                        new ApiCallingToolAskRespondedTransition())
                .addTransition(
                        TaskStatus.CALLING_API,
                        TaskStatus.CALLING_API,
                        TaskEventType.ASK_USER,
                        new AskUserTransition())
                .addTransition(
                        TaskStatus.CALLING_API,
                        TaskStatus.CALLING_API,
                        TaskEventType.API_CALLING_RETRY,
                        new ApiCallingTransition())
                .addTransition(
                        TaskStatus.CALLING_API,
                        TaskStatus.TASK_COMPLETE,
                        TaskEventType.API_CALLING_FAILED,
                        new TaskCompleteTransition())
                .addTransition(
                        TaskStatus.CALLING_API,
                        TaskStatus.CALLING_API,
                        TaskEventType.STREAMING_COMPLETE,
                        new StreamingCompleteTransition());
    }

    private static StateMachineFactory<TaskV2, TaskStatus, TaskEventType, TaskEvent>
            fromApiCompleted(StateMachineFactory<TaskV2, TaskStatus, TaskEventType, TaskEvent> f) {
        return f.addTransition(
                        TaskStatus.API_COMPLETED,
                        TaskStatus.PREPARE_CONTEXT,
                        TaskEventType.CONTINUE_NEXT_TURN,
                        new PrepareContextTransition())
                .addTransition(
                        TaskStatus.API_COMPLETED,
                        TaskStatus.PREPARE_CONTEXT,
                        TaskEventType.API_CALLING_RETRY,
                        new PrepareContextTransition())
                .addTransition(
                        TaskStatus.API_COMPLETED,
                        TaskStatus.WAITING_USER_ASK_RESPONSE,
                        TaskEventType.ASK_USER,
                        new AskUserTransition())
                .addTransition(
                        TaskStatus.API_COMPLETED,
                        TaskStatus.TASK_COMPLETE,
                        TaskEventType.TASK_COMPLETE,
                        new TaskCompleteTransition())
                .addTransition(
                        TaskStatus.API_COMPLETED,
                        TaskStatus.ABORT,
                        TaskEventType.ABORT,
                        new AbortTransition());
    }

    private static StateMachineFactory<TaskV2, TaskStatus, TaskEventType, TaskEvent>
            fromWaitingUserAskResponse(
                    StateMachineFactory<TaskV2, TaskStatus, TaskEventType, TaskEvent> f) {
        return f.addTransition(
                        TaskStatus.WAITING_USER_ASK_RESPONSE,
                        TaskStatus.WAITING_USER_ASK_RESPONSE,
                        TaskEventType.USER_RESPONDED,
                        new UserRespondedTransition())
                .addTransition(
                        TaskStatus.WAITING_USER_ASK_RESPONSE,
                        TaskStatus.TASK_COMPLETE,
                        TaskEventType.TASK_COMPLETE,
                        new TaskCompleteTransition())
                .addTransition(
                        TaskStatus.WAITING_USER_ASK_RESPONSE,
                        TaskStatus.PREPARE_CONTEXT,
                        TaskEventType.PREPARE_CONTEXT,
                        new PrepareContextTransition())
                .addTransition(
                        TaskStatus.WAITING_USER_ASK_RESPONSE,
                        TaskStatus.ABORT,
                        TaskEventType.ABORT,
                        new AbortTransition());
    }

    private static StateMachineFactory<TaskV2, TaskStatus, TaskEventType, TaskEvent> fromAbort(
            StateMachineFactory<TaskV2, TaskStatus, TaskEventType, TaskEvent> f) {
        return f.addTransition(
                        TaskStatus.ABORT,
                        TaskStatus.PREPARE_CONTEXT,
                        TaskEventType.RESTORE_TASK,
                        new PrepareContextTransition())
                .addTransition(
                        TaskStatus.ABORT,
                        TaskStatus.ABORT,
                        TaskEventType.ABORT,
                        new NoopTransition());
    }

    private static StateMachineFactory<TaskV2, TaskStatus, TaskEventType, TaskEvent>
            fromTaskComplete(StateMachineFactory<TaskV2, TaskStatus, TaskEventType, TaskEvent> f) {
        return f.addTransition(
                        TaskStatus.TASK_COMPLETE,
                        TaskStatus.START_TASK,
                        TaskEventType.START_TASK,
                        new StartTaskTransition())
                .addTransition(
                        TaskStatus.TASK_COMPLETE,
                        TaskStatus.TASK_COMPLETE,
                        TaskEventType.ABORT,
                        new NoopTransition());
    }
}
