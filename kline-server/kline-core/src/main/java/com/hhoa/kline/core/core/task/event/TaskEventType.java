package com.hhoa.kline.core.core.task.event;

public enum TaskEventType {
    START_TASK,

    RESUME_TASK,

    PREPARE_CONTEXT,

    CONTEXT_READY,

    PREPARE_FAILED,

    API_COMPLETED,

    API_CALLING_FAILED,

    USER_RESPONDED,

    CONTINUE_NEXT_TURN,

    TASK_COMPLETE,

    RETRY_PREPARE_CONTEXT,

    ABORT,

    RESTORE_TASK,

    API_CALLING_RETRY,

    ASK_USER,
}
