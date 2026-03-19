package com.hhoa.kline.core.core.task.state;

import com.hhoa.kline.core.core.task.TaskStatus;
import com.hhoa.kline.core.core.task.TaskV2;
import com.hhoa.kline.core.core.task.event.TaskEvent;
import com.hhoa.kline.core.core.task.statemachine.StateTransitionListener;

/** 状态迁移后持久化快照。在 postTransition 中根据当前 TaskV2 状态写入 TaskV2StateStore。 */
public class PersistOnTransitionListener
        implements StateTransitionListener<TaskV2, TaskEvent, TaskStatus> {

    private final String taskId;
    private final TaskV2StateStore store;

    public PersistOnTransitionListener(String taskId, TaskV2StateStore store) {
        this.taskId = taskId;
        this.store = store;
    }

    @Override
    public void preTransition(TaskV2 op, TaskStatus beforeState, TaskEvent eventToBeProcessed) {}

    @Override
    public void postTransition(
            TaskV2 op, TaskStatus beforeState, TaskStatus afterState, TaskEvent processedEvent) {
        TaskV2StateSnapshot snapshot =
                TaskV2StateSnapshot.builder()
                        .taskId(taskId)
                        .currentState(afterState)
                        .savedAtMillis(System.currentTimeMillis())
                        .build();
        store.save(snapshot);
    }
}
