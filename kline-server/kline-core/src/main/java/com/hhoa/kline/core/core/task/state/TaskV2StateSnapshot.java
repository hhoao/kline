package com.hhoa.kline.core.core.task.state;

import com.hhoa.kline.core.core.task.TaskStatus;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;

/**
 * 任务状态快照，用于持久化与恢复（参考 YARN RMStateStore 的恢复语义）。 恢复时还需配合 StateManager 的 clineMessages /
 * apiConversationHistory 使用。
 */
@Getter
@Builder
public class TaskV2StateSnapshot {

    private final String taskId;
    private final TaskStatus currentState;
    private final long savedAtMillis;

    @Builder.Default private final Map<String, Object> recoveryContext = Map.of();

    public TaskV2StateSnapshot(
            String taskId,
            TaskStatus currentState,
            long savedAtMillis,
            Map<String, Object> recoveryContext) {
        this.taskId = taskId;
        this.currentState = currentState;
        this.savedAtMillis = savedAtMillis;
        this.recoveryContext = recoveryContext != null ? recoveryContext : Map.of();
    }
}
