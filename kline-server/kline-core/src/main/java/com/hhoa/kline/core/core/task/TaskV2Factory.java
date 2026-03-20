package com.hhoa.kline.core.core.task;

public final class TaskV2Factory {

    private TaskV2Factory() {}

    public static TaskV2 create(TaskParams params) {
        TaskV2 task = new TaskV2(params);
        task.acquireTaskLock(params);
        task.initHandlers(params);
        task.initCheckpointManager();
        task.initMcpCallbacks();
        task.initStateMachine();
        return task;
    }
}
