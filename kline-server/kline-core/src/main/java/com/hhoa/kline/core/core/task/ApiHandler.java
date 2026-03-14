package com.hhoa.kline.core.core.task;

import com.hhoa.kline.core.core.assistant.MessageParam;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

/**
 * API 处理器接口
 *
 * @author hhoa
 */
public interface ApiHandler {

    /**
     * 获取最后一次请求的 Request ID
     *
     * @return 最后一次请求的 Request ID
     */
    String getLastRequestId();

    /**
     * 创建消息流（流式响应）
     *
     * @param systemPrompt 系统提示词
     * @param conversationHistory 对话历史
     * @return 流式的 ApiChunk（chunk 类型：text, tool_use, usage, reasoning 等）
     */
    Flux<ApiChunk> createMessageStream(String systemPrompt, List<MessageParam> conversationHistory);

    /**
     * 获取当前使用的模型 ID
     *
     * @return 模型 ID
     */
    String getModelId();

    /**
     * 获取提供者 ID
     *
     * @return 提供者 ID（如 "anthropic", "openai" 等）
     */
    String getProviderId();

    /**
     * 获取 API 流式响应使用情况
     *
     * @return API 流式响应使用情况
     */
    default ApiStreamUsage getApiStreamUsage() {
        Logger logger = LoggerFactory.getLogger(ApiHandler.class);
        logger.debug("getApiStreamUsage not implemented");
        return null;
    }

    interface ApiStreamUsage {
        int inputTokens();

        int outputTokens();

        int cacheWriteTokens();

        int cacheReadTokens();

        double totalCost();
    }
}
