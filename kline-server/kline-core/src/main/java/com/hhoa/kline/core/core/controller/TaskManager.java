package com.hhoa.kline.core.core.controller;

import com.hhoa.kline.core.core.services.mcp.IMcpHub;
import com.hhoa.kline.core.core.shared.ClineAskResponse;
import com.hhoa.kline.core.core.shared.ExtensionState;
import com.hhoa.kline.core.core.shared.storage.Settings;
import com.hhoa.kline.core.core.storage.StateManager;
import java.util.List;
import lombok.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * 统一的任务管理接口，方便在 V1 / V2 不同实现之间切换。
 *
 * <p>注意：避免在接口中暴露具体的 Task / TaskV2 类型，保持对上层调用方的稳定性。
 */
public interface TaskManager {

    String initTask(
            String text,
            List<String> images,
            List<String> files,
            HistoryItem historyItem,
            Settings taskSettings);

    void showTask(String taskId);

    void reinitExistingTaskFromId(String taskId);

    void reinitExistingTaskFromHistoryItem(HistoryItem historyItem);

    void cancelTask(String taskId);

    void cancelAllTask();

    void updateBackgroundCommandState(boolean running, String taskId);

    void cancelBackgroundCommand(String taskId);

    void exportTaskWithId(String id);

    void deleteTaskFromState(String taskId);

    void postStateToWebview(@Nullable String taskId);

    ExtensionState getStateToPostToWebview(@Nullable String taskId);

    boolean shouldShowBackgroundTerminalSuggestion();

    void dispose();

    StateManager getStateManager();

    void handleWebviewAskResponse(
            @NonNull String taskId,
            String pendingId,
            ClineAskResponse responseType,
            String text,
            List<String> images,
            List<String> files);

    void restoreCheckpoint(String taskId, Long number, String restoreType, Long offset);

    IMcpHub getMcpHub();
}
