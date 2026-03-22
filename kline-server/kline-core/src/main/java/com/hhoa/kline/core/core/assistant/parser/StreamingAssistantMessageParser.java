package com.hhoa.kline.core.core.assistant.parser;

import com.hhoa.kline.core.core.assistant.AssistantMessageContent;
import java.util.List;

/**
 * 有状态流式助手消息解析器
 *
 * <p>与 {@link AssistantMessageParser} 不同，该解析器维护内部状态，每次 {@link #feed(String)}
 * 只处理新增的文本块，避免重新解析整个消息，时间复杂度从 O(n²) 降至 O(n)。
 *
 * @author hhoa
 */
public interface StreamingAssistantMessageParser {
    /** 重置解析器状态，可用于开始解析新的消息流 */
    void reset();

    /**
     * 喂入新的文本块，返回当前所有已解析内容块的快照
     *
     * @param chunk 新的文本块
     * @return 当前所有内容块的快照
     */
    List<AssistantMessageContent> feed(String chunk);

    /**
     * 标记流结束，最终化所有块（将 partial 设为 false，处理未闭合的内容）
     *
     * @return 最终的内容块列表
     */
    List<AssistantMessageContent> complete();

    /**
     * 获取当前已解析的内容块快照（不改变解析器状态）
     *
     * @return 当前内容块的快照
     */
    List<AssistantMessageContent> getCurrentBlocks();

    /** 获取累积的原始消息文本 */
    String getAccumulatedMessage();

    /** 解析流是否已完成 */
    boolean isCompleted();
}
