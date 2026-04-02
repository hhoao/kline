package com.hhoa.kline.core.core.integrations.checkpoints;

import com.hhoa.kline.core.core.context.tracking.FileContextTracker;
import com.hhoa.kline.core.core.integrations.editor.DiffViewProvider;
import com.hhoa.kline.core.core.storage.StateManager;
import com.hhoa.kline.core.core.task.MessageStateHandler;
import com.hhoa.kline.core.core.task.TaskState;
import com.hhoa.kline.core.core.workspace.WorkspaceRootManager;
import com.hhoa.kline.core.subscription.MessageSender;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CheckpointManagerFactory {

    public static ICheckpointManager buildCheckpointManager(
            String taskId,
            MessageStateHandler messageStateHandler,
            FileContextTracker fileContextTracker,
            DiffViewProvider diffViewProvider,
            TaskState taskState,
            WorkspaceRootManager workspaceManager,
            TaskCheckpointManager.CheckpointManagerCallbacks callbacks,
            int[] initialConversationHistoryDeletedRange,
            String initialCheckpointManagerErrorMessage,
            StateManager stateManager,
            MessageSender messageSender) {

        boolean enableCheckpoints = stateManager.getSettings().isEnableCheckpointsSetting();

        if (shouldUseMultiRoot(workspaceManager, enableCheckpoints, stateManager)) {
            log.warn(
                    "Multi-root checkpoint manager not yet implemented, using single-root manager");
        }

        return new TaskCheckpointManager(
                taskId,
                enableCheckpoints,
                messageStateHandler,
                fileContextTracker,
                diffViewProvider,
                taskState,
                workspaceManager,
                callbacks,
                initialConversationHistoryDeletedRange,
                initialCheckpointManagerErrorMessage,
                stateManager,
                messageSender);
    }

    public static boolean shouldUseMultiRoot(
            WorkspaceRootManager workspaceManager,
            boolean enableCheckpoints,
            StateManager stateManager) {
        if (!enableCheckpoints || workspaceManager == null) {
            return false;
        }

        // Check if multi-root is enabled in settings
        Boolean multiRootEnabled = stateManager.getGlobalState().getMultiRootEnabled();
        if (multiRootEnabled == null || !multiRootEnabled) {
            return false;
        }

        // Check if there are multiple roots
        return workspaceManager.getRoots().size() > 1;
    }
}
