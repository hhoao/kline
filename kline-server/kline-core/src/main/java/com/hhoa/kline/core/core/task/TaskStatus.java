package com.hhoa.kline.core.core.task;

public enum TaskStatus {
    NEW,

    PREPARE_CONTEXT,

    CALLING_API,

    API_COMPLETED,

    TASK_COMPLETE,

    ABORT,

    START_TASK,

    WAITING_USER_ASK_RESPONSE
}
