package com.hhoa.kline.core.core.task;

import com.hhoa.kline.core.core.hooks.HookData;
import com.hhoa.kline.core.core.hooks.HookExecutionResult;
import com.hhoa.kline.core.core.hooks.HookExecutor;
import com.hhoa.kline.core.core.hooks.HookInput;
import com.hhoa.kline.core.core.hooks.HookName;
import com.hhoa.kline.core.core.shared.ClineSay;
import com.hhoa.kline.core.core.storage.StateManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

/**
 * 封装所有 hook 生命周期相关的回调和高级执行方法，避免 handler 构造函数膨胀。
 *
 * <p>提供 TaskStart、TaskResume、TaskCancel、UserPromptSubmit、Notification 等 hook 的统一执行入口。
 */
@Slf4j
@Builder
public class TaskHookSupport {

    private final String taskId;
    private final String ulid;

    /** say 回调：(type, text) */
    private final BiConsumer<ClineSay, String> say;

    /** 设置活跃 hook 执行（mutex 保护） */
    private final Consumer<HookExecution> setActiveHookExecution;

    /** 清除活跃 hook 执行（mutex 保护） */
    private final Runnable clearActiveHookExecution;

    /** hooks 是否启用的供应器 */
    private final Supplier<Boolean> hooksEnabled;

    /** hook 目录列表供应器 */
    private final Supplier<List<String>> hooksDirs;

    /** 工作区根目录列表供应器 */
    private final Supplier<List<String>> workspaceRoots;

    /** 取消任务回调 */
    private final Runnable cancelTask;

    /** 消息状态处理器 */
    private final MessageStateHandler messageStateHandler;

    /** 状态发布回调 */
    private final Runnable postStateToWebview;

    /** 状态管理器 */
    private final StateManager stateManager;

    /** 模型上下文供应器（可选） */
    private final Supplier<HookInput.HookModelContext> modelContext;

    // ---- 高级 hook 执行方法 ----

    /** 执行 TaskStart hook */
    public HookExecutionResult executeTaskStartHook(String initialTask) {
        if (!isHooksEnabled()) {
            return HookExecutionResult.empty();
        }

        Map<String, String> taskMetadata = new HashMap<>();
        taskMetadata.put("taskId", taskId);
        taskMetadata.put("ulid", ulid);
        taskMetadata.put("initialTask", initialTask != null ? initialTask : "");

        HookInput hookInput =
                HookInput.builder()
                        .hookName(HookName.TASK_START.getValue())
                        .taskId(taskId)
                        .workspaceRoots(workspaceRoots.get())
                        .taskStart(
                                HookData.TaskStartData.builder().taskMetadata(taskMetadata).build())
                        .model(getModelContext())
                        .build();

        return executeHookInternal(HookName.TASK_START, hookInput, true, null);
    }

    /** 执行 TaskResume hook */
    public HookExecutionResult executeTaskResumeHook(
            Long lastMessageTs, int messageCount, boolean conversationHistoryDeleted) {
        if (!isHooksEnabled()) {
            return HookExecutionResult.empty();
        }

        Map<String, String> taskMetadata = new HashMap<>();
        taskMetadata.put("taskId", taskId);
        taskMetadata.put("ulid", ulid);

        Map<String, String> previousState = new HashMap<>();
        previousState.put("lastMessageTs", lastMessageTs != null ? lastMessageTs.toString() : "");
        previousState.put("messageCount", String.valueOf(messageCount));
        previousState.put("conversationHistoryDeleted", String.valueOf(conversationHistoryDeleted));

        HookInput hookInput =
                HookInput.builder()
                        .hookName(HookName.TASK_RESUME.getValue())
                        .taskId(taskId)
                        .workspaceRoots(workspaceRoots.get())
                        .taskResume(
                                HookData.TaskResumeData.builder()
                                        .taskMetadata(taskMetadata)
                                        .previousState(previousState)
                                        .build())
                        .model(getModelContext())
                        .build();

        return executeHookInternal(HookName.TASK_RESUME, hookInput, true, null);
    }

    /** 执行 TaskCancel hook（不可取消） */
    public void executeTaskCancelHook(boolean abandoned) {
        if (!isHooksEnabled()) {
            return;
        }

        Map<String, String> taskMetadata = new HashMap<>();
        taskMetadata.put("taskId", taskId);
        taskMetadata.put("ulid", ulid);
        taskMetadata.put("completionStatus", abandoned ? "abandoned" : "cancelled");

        HookInput hookInput =
                HookInput.builder()
                        .hookName(HookName.TASK_CANCEL.getValue())
                        .taskId(taskId)
                        .workspaceRoots(workspaceRoots.get())
                        .taskCancel(
                                HookData.TaskCancelData.builder()
                                        .taskMetadata(taskMetadata)
                                        .build())
                        .model(getModelContext())
                        .build();

        try {
            // TaskCancel is NOT cancellable — no setActiveHookExecution/clearActiveHookExecution
            executeHookInternal(HookName.TASK_CANCEL, hookInput, false, null);
        } catch (Exception e) {
            log.error("[TaskCancel Hook] Failed (non-fatal):", e);
        }
    }

    /** 执行 UserPromptSubmit hook */
    public HookExecutionResult executeUserPromptSubmitHook(String prompt) {
        if (!isHooksEnabled()) {
            return HookExecutionResult.empty();
        }

        HookInput hookInput =
                HookInput.builder()
                        .hookName(HookName.USER_PROMPT_SUBMIT.getValue())
                        .taskId(taskId)
                        .workspaceRoots(workspaceRoots.get())
                        .userPromptSubmit(
                                HookData.UserPromptSubmitData.builder()
                                        .prompt(prompt != null ? prompt : "")
                                        .attachments(new ArrayList<>())
                                        .build())
                        .model(getModelContext())
                        .build();

        return executeHookInternal(HookName.USER_PROMPT_SUBMIT, hookInput, true, null);
    }

    /** 执行 Notification hook（异步，fire-and-forget） */
    public void executeNotificationHook(
            String event, String source, String message, boolean waitingForUserInput) {
        if (!isHooksEnabled()) {
            return;
        }

        CompletableFuture.runAsync(
                () -> {
                    try {
                        HookInput hookInput =
                                HookInput.builder()
                                        .hookName(HookName.NOTIFICATION.getValue())
                                        .taskId(taskId)
                                        .workspaceRoots(workspaceRoots.get())
                                        .notification(
                                                HookData.NotificationData.builder()
                                                        .event(event)
                                                        .source(source)
                                                        .message(message)
                                                        .waitingForUserInput(waitingForUserInput)
                                                        .build())
                                        .model(getModelContext())
                                        .build();

                        // Notification is NOT cancellable
                        executeHookInternal(HookName.NOTIFICATION, hookInput, false, null);
                    } catch (Exception e) {
                        log.error("[Notification Hook] Failed (non-fatal):", e);
                    }
                });
    }

    /** 统一的 hook 取消处理。确保状态始终在中止前保存，无论是用户点击取消还是 hook 返回 cancel:true。 */
    public void handleHookCancellation(String hookName, boolean wasCancelled) {
        try {
            messageStateHandler.saveClineMessagesAndUpdateHistory();
            messageStateHandler.overwriteApiConversationHistory(
                    messageStateHandler.getApiConversationHistory());
            if (postStateToWebview != null) {
                postStateToWebview.run();
            }
        } catch (Exception e) {
            log.error(
                    "[{}] Failed to save state during hook cancellation: {}",
                    hookName,
                    e.getMessage(),
                    e);
        }
    }

    // ---- internal ----

    private HookExecutionResult executeHookInternal(
            HookName hookName, HookInput hookInput, boolean cancellable, String toolName) {

        BiConsumer<String, String> sayCb =
                (type, text) -> {
                    try {
                        ClineSay clineSayType = ClineSay.valueOf(type.toUpperCase());
                        say.accept(clineSayType, text);
                    } catch (IllegalArgumentException e) {
                        log.warn("Unknown ClineSay type in hook callback: {}", type);
                    }
                };

        HookExecutor.HookExecutionOptions.HookExecutionOptionsBuilder builder =
                HookExecutor.HookExecutionOptions.builder()
                        .hookName(hookName)
                        .hookInput(hookInput)
                        .cancellable(cancellable)
                        .hooksEnabled(true) // already checked by caller
                        .taskId(taskId)
                        .toolName(toolName)
                        .hooksDirs(hooksDirs.get())
                        .workspaceRoots(workspaceRoots.get())
                        .say(sayCb);

        // Only set active hook tracking for cancellable hooks
        if (cancellable && setActiveHookExecution != null && clearActiveHookExecution != null) {
            builder.setActiveHookExecution(setActiveHookExecution);
            builder.clearActiveHookExecution(clearActiveHookExecution);
        }

        return HookExecutor.executeHook(builder.build());
    }

    private boolean isHooksEnabled() {
        try {
            return Boolean.TRUE.equals(hooksEnabled.get());
        } catch (Exception e) {
            log.debug("Failed to get hooksEnabled", e);
            return false;
        }
    }

    private HookInput.HookModelContext getModelContext() {
        if (modelContext != null) {
            return modelContext.get();
        }
        return null;
    }
}
