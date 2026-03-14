package com.hhoa.kline.core.core.task.focuschain;

import com.hhoa.kline.core.core.services.telemetry.TelemetryService;
import com.hhoa.kline.core.core.storage.StateManager;
import com.hhoa.kline.core.core.task.TaskState;
import com.hhoa.kline.core.core.task.focuschain.AbstractFocusChainManager.SayCallback;

public class LocalFocusChainManagerFactory implements FocusChainManagerFactory {
    private final StateManager stateManager;
    private final String tasksDirectory;

    public LocalFocusChainManagerFactory(StateManager stateManager, String tasksDirectory) {
        this.stateManager = stateManager;
        this.tasksDirectory = tasksDirectory;
    }

    @Override
    public FocusChainManager createFocusChainManager(
            String taskId,
            TaskState taskState,
            Runnable postStateToWebview,
            SayCallback say,
            TelemetryService telemetryService) {
        return new LocalFocusChainManager(
                taskId,
                taskState,
                tasksDirectory,
                stateManager,
                postStateToWebview,
                say,
                telemetryService);
    }
}
