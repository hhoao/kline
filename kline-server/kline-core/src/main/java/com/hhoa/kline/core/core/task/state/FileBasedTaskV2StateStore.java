package com.hhoa.kline.core.core.task.state;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.hhoa.kline.core.core.storage.StateManager;
import com.hhoa.kline.core.core.task.TaskStatus;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/** 基于任务目录下 taskv2-state.json 的持久化实现，依赖 StateManager 获取任务目录。 */
@Slf4j
public class FileBasedTaskV2StateStore implements TaskV2StateStore {

    private static final String STATE_FILE = "taskv2-state.json";
    private static final ObjectMapper MAPPER =
            new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private final StateManager stateManager;

    public FileBasedTaskV2StateStore(StateManager stateManager) {
        this.stateManager = stateManager;
    }

    @Override
    public void save(TaskV2StateSnapshot snapshot) {
        if (snapshot == null || snapshot.getTaskId() == null) {
            return;
        }
        try {
            String dir = stateManager.getOrCreateTaskDirectoryExists(snapshot.getTaskId());
            Path path = Paths.get(dir, STATE_FILE);
            Dto dto =
                    new Dto(
                            snapshot.getTaskId(),
                            snapshot.getCurrentState().name(),
                            snapshot.getSavedAtMillis(),
                            snapshot.getRecoveryContext());
            Files.writeString(path, MAPPER.writeValueAsString(dto), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Failed to save TaskV2 state: taskId={}", snapshot.getTaskId(), e);
        }
    }

    @Override
    public TaskV2StateSnapshot load(String taskId) {
        if (taskId == null) {
            return null;
        }
        try {
            String dir = stateManager.getOrCreateTaskDirectoryExists(taskId);
            Path path = Paths.get(dir, STATE_FILE);
            if (!Files.exists(path)) {
                return null;
            }
            String json = Files.readString(path, StandardCharsets.UTF_8);
            Dto dto = MAPPER.readValue(json, Dto.class);
            TaskStatus state = TaskStatus.valueOf(dto.currentState);
            return new TaskV2StateSnapshot(
                    dto.taskId,
                    state,
                    dto.savedAtMillis,
                    dto.recoveryContext != null ? dto.recoveryContext : Map.of());
        } catch (Exception e) {
            log.error("Failed to load TaskV2 state: taskId={}", taskId, e);
            return null;
        }
    }

    @Override
    public void delete(String taskId) {
        if (taskId == null) {
            return;
        }
        try {
            String dir = stateManager.getOrCreateTaskDirectoryExists(taskId);
            Path path = Paths.get(dir, STATE_FILE);
            Files.deleteIfExists(path);
        } catch (Exception e) {
            log.error("Failed to delete TaskV2 state: taskId={}", taskId, e);
        }
    }

    private static final class Dto {
        public String taskId;
        public String currentState;
        public long savedAtMillis;
        public Map<String, Object> recoveryContext;

        @SuppressWarnings("unused")
        public Dto() {}

        public Dto(
                String taskId,
                String currentState,
                long savedAtMillis,
                Map<String, Object> recoveryContext) {
            this.taskId = taskId;
            this.currentState = currentState;
            this.savedAtMillis = savedAtMillis;
            this.recoveryContext = recoveryContext;
        }
    }
}
