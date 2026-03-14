package com.hhoa.kline.core.core.task.focuschain;

import com.hhoa.kline.core.core.services.telemetry.TelemetryService;
import com.hhoa.kline.core.core.task.TaskState;
import com.hhoa.kline.core.core.task.focuschain.AbstractFocusChainManager.SayCallback;

public interface FocusChainManagerFactory {
    FocusChainManager createFocusChainManager(
            String taskId,
            TaskState taskState,
            Runnable postStateToWebview,
            SayCallback say,
            TelemetryService telemetryService);
}
