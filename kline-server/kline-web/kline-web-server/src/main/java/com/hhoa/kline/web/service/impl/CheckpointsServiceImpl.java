package com.hhoa.kline.web.service.impl;

import com.hhoa.kline.core.core.controller.TaskManager;
import com.hhoa.kline.core.core.controller.TaskManagerFactory;
import com.hhoa.kline.core.core.integrations.checkpoints.ICheckpointManager;
import com.hhoa.kline.core.core.task.Task;
import com.hhoa.kline.web.controller.dto.CheckpointRestoreRequestDTO;
import com.hhoa.kline.web.service.CheckpointsService;
import java.util.List;
import java.util.concurrent.TimeUnit;
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

        Task task = taskManager.getOrInitTask(taskId);
        long deadline = System.currentTimeMillis() + RESTORE_INIT_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            if (task.getTaskState().isInitialized()) {
                break;
            }
            try {
                TimeUnit.MILLISECONDS.sleep(POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("等待任务初始化被中断");
                throw new RuntimeException("Failed to restore checkpoint", e);
            }
        }
        if (!task.getTaskState().isInitialized()) {
            log.warn("任务 {} 在 {}ms 内未完成初始化，仍尝试恢复检查点", taskId, RESTORE_INIT_TIMEOUT_MS);
        }

        ICheckpointManager checkpointManager = task.getCheckpointManager();
        if (checkpointManager == null) {
            log.warn("任务 {} 无 checkpointManager，跳过恢复", taskId);
            return;
        }
        Integer offset = request.getOffset() != null ? request.getOffset().intValue() : null;
        checkpointManager
                .restoreCheckpoint(request.getNumber(), request.getRestoreType(), offset)
                .join();
    }

    @Override
    public String getCwdHash(List<String> request) {
        // TODO: 实现获取当前工作目录哈希逻辑
        return "";
    }
}
