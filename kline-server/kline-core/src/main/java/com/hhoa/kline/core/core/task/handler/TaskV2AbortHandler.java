package com.hhoa.kline.core.core.task.handler;

import com.hhoa.kline.core.core.integrations.terminal.TerminalManager;
import com.hhoa.kline.core.core.services.mcp.IMcpHub;
import com.hhoa.kline.core.core.task.HookExecution;
import com.hhoa.kline.core.core.task.MessageStateHandler;
import com.hhoa.kline.core.core.task.TaskHookSupport;
import com.hhoa.kline.core.core.task.TaskLockUtils;
import com.hhoa.kline.core.core.task.TaskState;
import com.hhoa.kline.core.core.task.deps.ActiveBackgroundCommand;
import com.hhoa.kline.core.core.task.focuschain.FocusChainManager;
import java.util.function.Consumer;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class TaskV2AbortHandler {

    private final TaskState taskState;
    private final FocusChainManager focusChainManager;
    private final Supplier<ActiveBackgroundCommand> getActiveBackgroundCommand;
    private final Runnable cancelBackgroundCommand;
    private final TerminalManager terminalManager;
    private final Supplier<Boolean> taskLockAcquiredGetter;
    private final Consumer<Boolean> taskLockAcquiredSetter;
    private final String taskId;
    private final IMcpHub mcpHub;
    private final TaskHookSupport hookSupport;
    private final MessageStateHandler messageStateHandler;
    private final Runnable postStateToWebview;
    private final Supplier<Boolean> shouldRunTaskCancelHook;

    public TaskV2AbortHandler(
            TaskState taskState,
            FocusChainManager focusChainManager,
            Supplier<ActiveBackgroundCommand> getActiveBackgroundCommand,
            Runnable cancelBackgroundCommand,
            TerminalManager terminalManager,
            Supplier<Boolean> taskLockAcquiredGetter,
            Consumer<Boolean> taskLockAcquiredSetter,
            String taskId,
            IMcpHub mcpHub,
            TaskHookSupport hookSupport,
            MessageStateHandler messageStateHandler,
            Runnable postStateToWebview,
            Supplier<Boolean> shouldRunTaskCancelHook) {
        this.taskState = taskState;
        this.focusChainManager = focusChainManager;
        this.getActiveBackgroundCommand = getActiveBackgroundCommand;
        this.cancelBackgroundCommand = cancelBackgroundCommand;
        this.terminalManager = terminalManager;
        this.taskLockAcquiredGetter = taskLockAcquiredGetter;
        this.taskLockAcquiredSetter = taskLockAcquiredSetter;
        this.taskId = taskId;
        this.mcpHub = mcpHub;
        this.hookSupport = hookSupport;
        this.messageStateHandler = messageStateHandler;
        this.postStateToWebview = postStateToWebview;
        this.shouldRunTaskCancelHook = shouldRunTaskCancelHook;
    }

    public void abort() {
        try {
            // PHASE 1: Check if TaskCancel hook should run BEFORE any cleanup
            // We must capture this state now because subsequent cleanup will
            // clear the active work indicators that shouldRunTaskCancelHook checks
            boolean shouldRunCancelHook = false;
            if (shouldRunTaskCancelHook != null) {
                try {
                    shouldRunCancelHook = Boolean.TRUE.equals(shouldRunTaskCancelHook.get());
                } catch (Exception e) {
                    log.debug("Failed to check shouldRunTaskCancelHook", e);
                }
            }

            // PHASE 2: Set abort flag to prevent race conditions
            // This must happen before canceling hooks so that catch blocks
            // can properly detect the abort state
            taskState.setAbort(true);

            // PHASE 3: Cancel any running hook execution
            HookExecution activeHook = taskState.getActiveHookExecution();
            if (activeHook != null) {
                try {
                    activeHook.cancel();
                    taskState.setActiveHookExecution(null);
                } catch (Exception e) {
                    log.error("Failed to cancel hook during task abort", e);
                    taskState.setActiveHookExecution(null);
                }
            }

            // PHASE 4: Cancel background command
            if (getActiveBackgroundCommand.get() != null) {
                try {
                    cancelBackgroundCommand.run();
                } catch (Exception e) {
                    log.error("Failed to cancel background command during task abort", e);
                }
            }

            // PHASE 5: Run TaskCancel hook (non-cancellable)
            if (hookSupport != null && shouldRunCancelHook) {
                try {
                    hookSupport.executeTaskCancelHook(taskState.isAbandoned());
                } catch (Exception e) {
                    log.error("[TaskCancel Hook] Failed (non-fatal):", e);
                }
            }

            // PHASE 6: Save state and update UI
            try {
                if (messageStateHandler != null) {
                    messageStateHandler.saveClineMessagesAndUpdateHistory();
                }
                if (postStateToWebview != null) {
                    postStateToWebview.run();
                }
            } catch (Exception e) {
                log.error("Failed to save state after abort", e);
            }

            // PHASE 7: Check for incomplete progress
            if (focusChainManager != null) {
                focusChainManager.checkIncompleteProgressOnCompletion();
            }

            // PHASE 8: Clean up resources
            if (terminalManager != null) {
                try {
                    terminalManager.close();
                } catch (Exception e) {
                    log.error("Failed to close terminal manager: {}", e.getMessage(), e);
                }
            }

            if (focusChainManager != null) {
                focusChainManager.dispose();
            }

            if (mcpHub != null) {
                mcpHub.clearNotificationCallback();
            }
        } finally {
            // Release task folder lock
            if (Boolean.TRUE.equals(taskLockAcquiredGetter.get())) {
                try {
                    TaskLockUtils.releaseTaskLock(taskId);
                    taskLockAcquiredSetter.accept(false);
                    log.info("[Task {}] Task lock released", taskId);
                } catch (Exception e) {
                    log.error(
                            "[Task {}] Failed to release task lock: {}", taskId, e.getMessage(), e);
                }
            }
        }
    }
}
