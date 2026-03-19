package com.hhoa.kline.core.core.task.state;

/** TaskV2 状态存储接口。持久化/加载状态快照，恢复时与 StateManager 的 clineMessages、apiConversationHistory 配合使用。 */
public interface TaskV2StateStore {

    void save(TaskV2StateSnapshot snapshot);

    TaskV2StateSnapshot load(String taskId);

    void delete(String taskId);
}
