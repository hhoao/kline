package com.hhoa.kline.core.core.task.state;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryTaskV2StateStore implements TaskV2StateStore {

    private final Map<String, TaskV2StateSnapshot> map = new ConcurrentHashMap<>();

    @Override
    public void save(TaskV2StateSnapshot snapshot) {
        if (snapshot == null || snapshot.getTaskId() == null) {
            return;
        }
        map.put(snapshot.getTaskId(), snapshot);
    }

    @Override
    public TaskV2StateSnapshot load(String taskId) {
        return taskId == null ? null : map.get(taskId);
    }

    @Override
    public void delete(String taskId) {
        if (taskId != null) {
            map.remove(taskId);
        }
    }
}
