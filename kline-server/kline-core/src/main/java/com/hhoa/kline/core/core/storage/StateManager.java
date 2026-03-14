package com.hhoa.kline.core.core.storage;

import com.hhoa.kline.core.core.assistant.MessageParam;
import com.hhoa.kline.core.core.context.tracking.TaskMetadata;
import com.hhoa.kline.core.core.controller.HistoryItem;
import com.hhoa.kline.core.core.shared.storage.GlobalState;
import com.hhoa.kline.core.core.shared.storage.Secrets;
import com.hhoa.kline.core.core.shared.storage.Settings;
import com.hhoa.kline.core.core.task.ClineMessage;
import java.io.IOException;
import java.util.List;

/**
 * 状态管理器接口 提供全局状态、密钥、设置和任务相关的存储管理功能
 *
 * @author hhoa
 */
public interface StateManager {

    /**
     * 获取GlobalState（使用缓存）
     *
     * @return GlobalState对象
     */
    GlobalState getGlobalState();

    /**
     * 获取Secrets（使用缓存）
     *
     * @return Secrets对象
     */
    Secrets getSecrets();

    /**
     * 获取Settings（使用缓存）
     *
     * @return Settings对象
     */
    Settings getSettings();

    /**
     * 更新GlobalState
     *
     * @param globalState GlobalState对象
     */
    void updateGlobalState(GlobalState globalState);

    /**
     * 更新Secrets
     *
     * @param secrets Secrets对象
     */
    void updateSecrets(Secrets secrets);

    /**
     * 更新Settings
     *
     * @param settings Settings对象
     */
    void updateSettings(Settings settings);

    /** 关闭StateManager，停止定时任务并持久化数据 */
    void shutdown();

    /**
     * 确保任务目录存在
     *
     * @param taskId 任务ID
     * @return 任务目录路径
     */
    String getOrCreateTaskDirectoryExists(String taskId);

    /**
     * 获取任务目录大小
     *
     * @param taskId 任务ID
     * @return 任务目录大小（字节）
     */
    long getTaskDirectorySize(String taskId);

    /**
     * 获取保存的API对话历史
     *
     * @param taskId 任务ID
     * @return API对话历史列表
     */
    List<MessageParam> getSavedApiConversationHistory(String taskId);

    /**
     * 保存API对话历史
     *
     * @param taskId 任务ID
     * @param apiConversationHistory API对话历史
     */
    void saveApiConversationHistory(String taskId, List<MessageParam> apiConversationHistory);

    /**
     * 获取保存的Cline消息
     *
     * @param taskId 任务ID
     * @return Cline消息列表
     */
    List<ClineMessage> getSavedClineMessages(String taskId);

    /**
     * 保存Cline消息
     *
     * @param taskId 任务ID
     * @param uiMessages UI消息列表
     */
    void saveClineMessages(String taskId, List<ClineMessage> uiMessages);

    /**
     * 获取任务元数据
     *
     * @param taskId 任务ID
     * @return TaskMetadata 对象
     */
    TaskMetadata getTaskMetadata(String taskId);

    /**
     * 保存任务元数据
     *
     * @param taskId 任务ID
     * @param metadata TaskMetadata 对象
     */
    void saveTaskMetadata(String taskId, TaskMetadata metadata);

    /**
     * 删除任务
     *
     * @param taskId 任务ID
     */
    void deleteTask(String taskId);

    /**
     * 获取任务历史
     *
     * @param taskId 任务ID
     * @return 任务历史列表
     */
    List<HistoryItem> getTaskHistory(String taskId);

    List<String> getWorkspaceRoots();

    long getTotalTasksSize();

    /**
     * 获取全局 Cline 规则目录路径（用于 .clinerules 全局规则）
     *
     * @return 目录路径，如果实现不支持则返回 null
     */
    default String getGlobalClineRulesDirectory() {
        return null;
    }

    String getFocusChain(String taskId);

    void saveFocusChain(String taskId, String todoList) throws IOException;
}
