package com.hhoa.kline.web.service.impl;

import com.hhoa.kline.core.core.controller.HistoryItem;
import com.hhoa.kline.core.core.controller.TaskManager;
import com.hhoa.kline.core.core.controller.TaskManagerFactory;
import com.hhoa.kline.core.core.services.telemetry.DefaultTelemetryService;
import com.hhoa.kline.core.core.services.telemetry.TelemetryService;
import com.hhoa.kline.core.core.shared.ClineAskResponse;
import com.hhoa.kline.core.core.shared.proto.cline.TaskHistoryArray;
import com.hhoa.kline.core.core.shared.proto.cline.TaskItem;
import com.hhoa.kline.core.core.shared.storage.Settings;
import com.hhoa.kline.core.core.task.Task;
import com.hhoa.kline.web.common.utils.object.BeanUtils;
import com.hhoa.kline.web.controller.dto.AskResponseRequestDTO;
import com.hhoa.kline.web.controller.dto.DeleteTasksRequestDTO;
import com.hhoa.kline.web.controller.dto.ExportTaskRequestDTO;
import com.hhoa.kline.web.controller.dto.GetTaskHistoryRequestDTO;
import com.hhoa.kline.web.controller.dto.NewTaskRequestDTO;
import com.hhoa.kline.web.controller.dto.ShowTaskRequestDTO;
import com.hhoa.kline.web.controller.dto.TaskCompletionViewChangesRequestDTO;
import com.hhoa.kline.web.controller.dto.TaskFeedbackRequestDTO;
import com.hhoa.kline.web.controller.dto.ToggleTaskFavoriteRequestDTO;
import com.hhoa.kline.web.service.TaskService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final TaskManagerFactory taskManagerFactory;

    @Override
    public String newTask(NewTaskRequestDTO request) {
        Settings taskSettings = request.getTaskSettings();

        return taskManagerFactory
                .getOrCreateTaskManager()
                .initTask(
                        request.getText(),
                        request.getImages() != null ? new ArrayList<>(request.getImages()) : null,
                        request.getFiles() != null ? new ArrayList<>(request.getFiles()) : null,
                        null,
                        taskSettings);
    }

    @Override
    public void cancelTask(String taskId) {
        taskManagerFactory.getOrCreateTaskManager().cancelTask(taskId);
    }

    @Override
    public void cancelBackgroundCommand(String taskId) {
        taskManagerFactory.getOrCreateTaskManager().cancelBackgroundCommand(taskId);
    }

    @Override
    public void clearTask(String taskId) {
        TaskManager orCreateTaskManager = taskManagerFactory.getOrCreateTaskManager();
        orCreateTaskManager.postStateToWebview(null);
    }

    @Override
    public Long getTotalTasksSize() {
        return taskManagerFactory.getOrCreateTaskManager().getStateManager().getTotalTasksSize();
    }

    @Override
    public void deleteTasksWithIds(DeleteTasksRequestDTO request) {
        if (request.getValue() == null || request.getValue().isEmpty()) {
            throw new IllegalArgumentException("Missing task IDs");
        }
        TaskManager taskManager = taskManagerFactory.getOrCreateTaskManager();

        for (String id : request.getValue()) {
            taskManager.deleteTaskFromState(id);
        }
    }

    @Override
    public void showTaskWithId(ShowTaskRequestDTO request) {
        TaskManager taskManager = taskManagerFactory.getOrCreateTaskManager();
        taskManager.showTask(request.getTaskId());
    }

    @Override
    public void exportTaskWithId(ExportTaskRequestDTO request) {
        if (request.getValue() != null) {
            taskManagerFactory.getOrCreateTaskManager().exportTaskWithId(request.getValue());
        }
    }

    @Override
    public void toggleTaskFavorite(ToggleTaskFavoriteRequestDTO request) {
        TaskManager taskManager = taskManagerFactory.getOrCreateTaskManager();

        List<HistoryItem> taskHistory =
                taskManager.getStateManager().getTaskHistory(request.getTaskId());

        if (taskHistory == null || taskHistory.isEmpty()) {
            return;
        }
        HistoryItem historyItem = taskHistory.getFirst();
        if (historyItem == null) {
            return;
        }
        historyItem.setFavorited(request.getFavorited());
        taskManager.postStateToWebview(request.getTaskId());
    }

    @Override
    public TaskHistoryArray getTaskHistory(GetTaskHistoryRequestDTO request) {
        TaskManager taskManager = taskManagerFactory.getOrCreateTaskManager();

        List<HistoryItem> taskHistory =
                taskManager.getStateManager().getGlobalState().getTaskHistory();

        List<TaskItem> tasks =
                taskHistory.stream()
                        .map(historyItem -> BeanUtils.toBean(historyItem, TaskItem.class))
                        .collect(Collectors.toList());

        return TaskHistoryArray.builder().tasks(tasks).totalCount(taskHistory.size()).build();
    }

    @Override
    public void askResponse(AskResponseRequestDTO request) {
        TaskManager taskManager = taskManagerFactory.getOrCreateTaskManager();

        ClineAskResponse responseType = ClineAskResponse.fromValue(request.getResponseType());
        if (responseType == null) {
            return;
        }

        Task task = taskManager.getOrInitTask(request.getTaskId());

        task.handleWebviewAskResponse(
                responseType,
                request.getText(),
                request.getImages() != null ? new ArrayList<>(request.getImages()) : null,
                request.getFiles() != null ? new ArrayList<>(request.getFiles()) : null);
    }

    @Override
    public void taskFeedback(TaskFeedbackRequestDTO request) {
        if (request.getValue() == null) {
            return;
        }

        TelemetryService telemetryService = new DefaultTelemetryService();

        Map<String, Object> properties = new HashMap<>();
        properties.put("feedbackType", request.getValue());
        telemetryService.captureEvent(request.getTaskId(), "task_feedback", properties);
        log.info("Task feedback: {} for task {}", request.getValue(), request.getTaskId());
    }

    @Override
    public void taskCompletionViewChanges(TaskCompletionViewChangesRequestDTO request) {}

    @Override
    public Integer deleteAllTaskHistory() {
        TaskManager taskManager = taskManagerFactory.getOrCreateTaskManager();

        List<HistoryItem> taskHistory =
                taskManager.getStateManager().getGlobalState().getTaskHistory();
        int totalTasks = taskHistory.size();

        for (HistoryItem historyItem : taskHistory) {
            taskManager.deleteTaskFromState(historyItem.getId());
        }

        return totalTasks;
    }
}
