package com.hhoa.kline.core.core.task.handler;

import com.hhoa.kline.core.core.integrations.terminal.TerminalManager;
import com.hhoa.kline.core.core.services.mcp.IMcpHub;
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

    public TaskV2AbortHandler(
            TaskState taskState,
            FocusChainManager focusChainManager,
            Supplier<ActiveBackgroundCommand> getActiveBackgroundCommand,
            Runnable cancelBackgroundCommand,
            TerminalManager terminalManager,
            Supplier<Boolean> taskLockAcquiredGetter,
            Consumer<Boolean> taskLockAcquiredSetter,
            String taskId,
            IMcpHub mcpHub) {
        this.taskState = taskState;
        this.focusChainManager = focusChainManager;
        this.getActiveBackgroundCommand = getActiveBackgroundCommand;
        this.cancelBackgroundCommand = cancelBackgroundCommand;
        this.terminalManager = terminalManager;
        this.taskLockAcquiredGetter = taskLockAcquiredGetter;
        this.taskLockAcquiredSetter = taskLockAcquiredSetter;
        this.taskId = taskId;
        this.mcpHub = mcpHub;
    }

    public void abort() {
        if (taskState.isAbort()) {
            return;
        }

        if (focusChainManager != null) {
            focusChainManager.checkIncompleteProgressOnCompletion();
        }

        taskState.setAbort(true);

        if (getActiveBackgroundCommand.get() != null) {
            cancelBackgroundCommand.run();
        }

        if (terminalManager != null) {
            try {
                terminalManager.close();
            } catch (Exception e) {
                log.error("Failed to close terminal manager: {}", e.getMessage(), e);
            }
        }

        if (Boolean.TRUE.equals(taskLockAcquiredGetter.get())) {
            TaskLockUtils.releaseTaskLock(taskId);
            taskLockAcquiredSetter.accept(false);
        }

        if (focusChainManager != null) {
            focusChainManager.dispose();
        }

        if (mcpHub != null) {
            mcpHub.clearNotificationCallback();
        }
    }
}
