package com.hhoa.kline.core.core.task.focuschain;

/** Focus Chain 管理器接口 负责管理任务进度列表（Focus Chain）的创建、更新和监控 */
public interface FocusChainManager {
    /**
     * 判断是否应该在 AI 提示中包含 Focus Chain 指令
     *
     * @return 如果应该包含 Focus Chain 指令则返回 true
     */
    boolean shouldIncludeFocusChainInstructions();

    /**
     * 生成 Focus Chain 指令文本，用于注入模型提示
     *
     * @return Focus Chain 指令文本
     */
    String generateFocusChainInstructions();

    /**
     * 从工具响应更新 Focus Chain 列表
     *
     * @param todoList 任务进度文本
     */
    void updateFCListFromToolResponse(String todoList);

    /** 设置文件监听器以监控 Focus Chain 列表 markdown 文件的变化 当文件被创建、修改或删除时自动更新 UI */
    void setupFocusChain();

    /** 分析任务标记为完成时 Focus Chain 列表中未完成的项目 捕获有关未完成进度项的遥测数据以帮助改进 Focus Chain 系统 */
    void checkIncompleteProgressOnCompletion();

    /** 执行清理操作，当 Focus Chain 管理器不再需要时 取消活动文件监听器并清除任何待处理的防抖定时器以防止内存泄漏 */
    void dispose();
}
