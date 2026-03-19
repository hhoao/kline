package com.hhoa.kline.web.service.impl;

import com.hhoa.kline.core.core.controller.TaskManager;
import com.hhoa.kline.core.core.controller.TaskManagerFactory;
import com.hhoa.kline.web.controller.dto.CheckpointRestoreRequestDTO;
import com.hhoa.kline.web.service.CheckpointsService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class CheckpointsServiceImpl implements CheckpointsService {

    private static final int RESTORE_INIT_TIMEOUT_MS = 3_000;
    private static final int POLL_INTERVAL_MS = 50;

    private final TaskManagerFactory taskManagerFactory;

    @Override
    public void checkpointDiff(Long request) {
        // TODO: 实现检查点差异逻辑
    }

    @Override
    public void checkpointRestore(CheckpointRestoreRequestDTO request) {
        String taskId = request.getTaskId();
        if (taskId == null || taskId.isBlank()) {
            throw new IllegalArgumentException("checkpointRestore 需要 taskId");
        }
        TaskManager taskManager = taskManagerFactory.getOrCreateTaskManager();
        taskManager.cancelTask(taskId);

        if (request.getNumber() == null || request.getNumber() == 0) {
            return;
        }

        taskManager.restoreCheckpoint(
                taskId, request.getNumber(), request.getRestoreType(), request.getOffset());
    }

    @Override
    public String getCwdHash(List<String> request) {
        // TODO: 实现获取当前工作目录哈希逻辑
        return "";
    }
}
