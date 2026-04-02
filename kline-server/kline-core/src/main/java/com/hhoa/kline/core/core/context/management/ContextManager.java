package com.hhoa.kline.core.core.context.management;

import com.hhoa.kline.core.core.assistant.MessageParam;
import com.hhoa.kline.core.core.task.ClineMessage;
import java.util.List;

/**
 * ContextManager
 *
 * @author xianxing
 * @since 2026/1/24
 */
public interface ContextManager {
    /**
     * 确定是否应该压缩上下文窗口，基于令牌计数。 使用固定的 0.75 阈值（与上游 cline 一致）。
     *
     * @param clineMessages Cline 消息列表
     * @param contextWindow 上下文窗口大小
     * @param maxAllowedSize 最大允许大小
     * @param previousApiReqIndex 上一个 API 请求索引
     * @return 如果应该压缩则返回 true
     */
    boolean shouldCompactContextWindow(
            List<ClineMessage> clineMessages,
            int contextWindow,
            int maxAllowedSize,
            int previousApiReqIndex);

    /**
     * 获取新的上下文消息和元数据的主要入口点
     *
     * @param apiConversationHistory API 对话历史
     * @param clineMessages Cline 消息列表
     * @param contextWindow 上下文窗口大小
     * @param maxAllowedSize 最大允许大小
     * @param conversationHistoryDeletedRange 对话历史删除范围
     * @param previousApiReqIndex 上一个 API 请求索引
     * @param useAutoCondense 是否使用自动压缩
     * @return 包含更新后的对话历史删除范围、是否更新标志和截断后的对话历史的结果
     */
    ContextMessagesResult getNewContextMessagesAndMetadata(
            List<MessageParam> apiConversationHistory,
            List<ClineMessage> clineMessages,
            int contextWindow,
            int maxAllowedSize,
            int[] conversationHistoryDeletedRange,
            int previousApiReqIndex,
            boolean useAutoCondense);

    /**
     * 获取上下文遥测数据，用于上下文管理决策 返回驱动摘要的令牌计数和上下文窗口信息
     *
     * @param clineMessages Cline 消息列表
     * @param contextWindow 上下文窗口大小
     * @param triggerIndex 触发索引（可选）
     * @return 遥测数据，如果无法获取则返回 null
     */
    ContextTelemetryData getContextTelemetryData(
            List<ClineMessage> clineMessages, int contextWindow, Integer triggerIndex);

    /**
     * 获取下一个截断范围
     *
     * @param apiMessages API 消息列表
     * @param currentDeletedRange 当前删除范围
     * @param keep 保留策略："none"、"lastTwo"、"half"、"quarter"
     * @return 截断范围 [startIndex, endIndex]（包含）
     */
    int[] getNextTruncationRange(
            List<MessageParam> apiMessages, int[] currentDeletedRange, KeepStrategy keep);

    /**
     * 获取截断的消息（外部接口，支持旧调用）
     *
     * @param messages 消息列表
     * @param deletedRange 删除范围
     * @return 截断后的消息列表
     */
    List<MessageParam> getTruncatedMessages(List<MessageParam> messages, int[] deletedRange);

    /**
     * 删除指定时间戳之后发生的所有上下文历史更新并保存到磁盘
     *
     * @param timestamp 时间戳
     */
    void truncateContextHistory(long timestamp);

    /**
     * 公共函数，用于可能触发设置截断消息 如果截断消息已存在，则不执行任何操作，否则添加消息
     *
     * @param timestamp 时间戳
     * @param apiConversationHistory API 对话历史
     */
    void triggerApplyStandardContextTruncationNoticeChange(
            long timestamp, List<MessageParam> apiConversationHistory);

    public static class ContextMessagesResult {
        private final int[] conversationHistoryDeletedRange;
        private final boolean updatedConversationHistoryDeletedRange;
        private final List<MessageParam> truncatedConversationHistory;

        public ContextMessagesResult(
                int[] conversationHistoryDeletedRange,
                boolean updatedConversationHistoryDeletedRange,
                List<MessageParam> truncatedConversationHistory) {
            this.conversationHistoryDeletedRange = conversationHistoryDeletedRange;
            this.updatedConversationHistoryDeletedRange = updatedConversationHistoryDeletedRange;
            this.truncatedConversationHistory = truncatedConversationHistory;
        }

        public int[] getConversationHistoryDeletedRange() {
            return conversationHistoryDeletedRange;
        }

        public boolean isUpdatedConversationHistoryDeletedRange() {
            return updatedConversationHistoryDeletedRange;
        }

        public List<MessageParam> getTruncatedConversationHistory() {
            return truncatedConversationHistory;
        }
    }

    /** 用于从磁盘加载 contextHistoryUpdates 的公共函数（如果存在） */
    void initializeContextHistory();
}
