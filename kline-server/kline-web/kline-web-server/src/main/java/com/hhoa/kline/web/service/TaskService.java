package com.hhoa.kline.web.service;

import com.hhoa.kline.core.core.shared.proto.cline.TaskHistoryArray;
import com.hhoa.kline.web.controller.dto.AskResponseRequestDTO;
import com.hhoa.kline.web.controller.dto.DeleteTasksRequestDTO;
import com.hhoa.kline.web.controller.dto.ExportTaskRequestDTO;
import com.hhoa.kline.web.controller.dto.GetTaskHistoryRequestDTO;
import com.hhoa.kline.web.controller.dto.NewTaskRequestDTO;
import com.hhoa.kline.web.controller.dto.ShowTaskRequestDTO;
import com.hhoa.kline.web.controller.dto.TaskCompletionViewChangesRequestDTO;
import com.hhoa.kline.web.controller.dto.TaskFeedbackRequestDTO;
import com.hhoa.kline.web.controller.dto.ToggleTaskFavoriteRequestDTO;

public interface TaskService {

    String newTask(NewTaskRequestDTO request);

    void cancelTask(String taskId);

    void cancelBackgroundCommand(String taskId);

    void clearTask(String taskId);

    Long getTotalTasksSize();

    void deleteTasksWithIds(DeleteTasksRequestDTO request);

    void showTaskWithId(ShowTaskRequestDTO request);

    void exportTaskWithId(ExportTaskRequestDTO request);

    void toggleTaskFavorite(ToggleTaskFavoriteRequestDTO request);

    TaskHistoryArray getTaskHistory(GetTaskHistoryRequestDTO request);

    void askResponse(AskResponseRequestDTO request);

    void taskFeedback(TaskFeedbackRequestDTO request);

    void taskCompletionViewChanges(TaskCompletionViewChangesRequestDTO request);

    Integer deleteAllTaskHistory();
}
