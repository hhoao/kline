package com.hhoa.kline.core.core.task.event;

/** 任务事件抽象基类（参考 Hadoop YARN AbstractEvent / RMAppEvent），子类对应不同事件类型与 payload。 */
public abstract class TaskEvent {

    private final TaskEventType type;
    private final String taskId;
    private final long timestamp;

    protected TaskEvent(TaskEventType type, String taskId) {
        this(type, taskId, System.currentTimeMillis());
    }

    protected TaskEvent(TaskEventType type, String taskId, long timestamp) {
        this.type = type;
        this.taskId = taskId;
        this.timestamp = timestamp;
    }

    public TaskEventType getType() {
        return type;
    }

    public String getTaskId() {
        return taskId;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
