package com.hhoa.kline.web.controller;

import static com.hhoa.kline.web.common.pojo.CommonResult.success;

import com.hhoa.kline.core.core.shared.proto.cline.TaskHistoryArray;
import com.hhoa.kline.web.common.pojo.CommonResult;
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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ExecutionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "Cline 任务服务")
@RestController
@RequestMapping("/api/cline/task")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @Operation(summary = "创建新任务")
    @PostMapping("/new")
    public CommonResult<String> newTask(@Valid @RequestBody NewTaskRequestDTO request) {
        String response = taskService.newTask(request);
        return success(response);
    }

    @Operation(summary = "取消当前运行的任务")
    @PostMapping("/cancel")
    public CommonResult<Void> cancelTask(@Valid @NotBlank String taskId) {
        taskService.cancelTask(taskId);
        return success(null);
    }

    @Operation(summary = "取消后台命令")
    @PostMapping("/cancel-background-command")
    public CommonResult<Void> cancelBackgroundCommand(String taskId) {
        taskService.cancelBackgroundCommand(taskId);
        return success(null);
    }

    @PostMapping("/clear")
    public CommonResult<Void> clearTask(String taskId) {
        taskService.clearTask(taskId);
        return success(null);
    }

    @Operation(summary = "获取所有任务的总大小")
    @GetMapping("/total-size")
    public CommonResult<Long> getTotalTasksSize() {
        Long response = taskService.getTotalTasksSize();
        return success(response);
    }

    @Operation(summary = "删除指定ID的任务")
    @PostMapping("/delete")
    public CommonResult<Void> deleteTasksWithIds(@Valid @RequestBody DeleteTasksRequestDTO request)
            throws IOException, ExecutionException, InterruptedException {
        taskService.deleteTasksWithIds(request);
        return success(null);
    }

    @Operation(summary = "显示指定ID的任务")
    @PostMapping("/show")
    public CommonResult<Void> showTaskWithId(@Valid @RequestBody ShowTaskRequestDTO request) {
        taskService.showTaskWithId(request);
        return success(null);
    }

    @Operation(summary = "导出指定ID的任务")
    @PostMapping("/export")
    public CommonResult<Void> exportTaskWithId(@Valid @RequestBody ExportTaskRequestDTO request) {
        taskService.exportTaskWithId(request);
        return success(null);
    }

    @Operation(summary = "切换任务收藏状态")
    @PostMapping("/toggle-favorite")
    public CommonResult<Void> toggleTaskFavorite(
            @Valid @RequestBody ToggleTaskFavoriteRequestDTO request) {
        taskService.toggleTaskFavorite(request);
        return success(null);
    }

    @Operation(summary = "获取任务历史")
    @PostMapping("/history")
    public CommonResult<TaskHistoryArray> getTaskHistory(
            @Valid @RequestBody GetTaskHistoryRequestDTO request) {
        TaskHistoryArray response = taskService.getTaskHistory(request);
        return success(response);
    }

    @Operation(summary = "询问响应")
    @PostMapping("/ask-response")
    public CommonResult<Void> askResponse(@Valid @RequestBody AskResponseRequestDTO request) {
        taskService.askResponse(request);
        return success(null);
    }

    @Operation(summary = "任务反馈")
    @PostMapping("/feedback")
    public CommonResult<Void> taskFeedback(@Valid @RequestBody TaskFeedbackRequestDTO request) {
        taskService.taskFeedback(request);
        return success(null);
    }

    @Operation(summary = "任务完成查看更改")
    @PostMapping("/completion-view-changes")
    public CommonResult<Void> taskCompletionViewChanges(
            @Valid @RequestBody TaskCompletionViewChangesRequestDTO request)
            throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        taskService.taskCompletionViewChanges(request);
        return success(null);
    }

    @Operation(summary = "删除所有任务历史")
    @PostMapping("/delete-all-history")
    public CommonResult<Integer> deleteAllTaskHistory() {
        Integer response = taskService.deleteAllTaskHistory();
        return success(response);
    }
}
