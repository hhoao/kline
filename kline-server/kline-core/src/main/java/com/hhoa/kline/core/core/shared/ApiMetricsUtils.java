package com.hhoa.kline.core.core.shared;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hhoa.kline.core.core.task.ClineMessage;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 从 ClineMessage 数组计算 API 指标。
 *
 * <p>此函数处理由 combineApiRequests 函数组合的 'api_req_started' 消息及其对应的 'api_req_finished' 消息。 它还考虑
 * 'deleted_api_reqs' 消息，这些消息是从已删除的消息聚合而来的。 它从这些消息中提取并汇总 tokensIn、tokensOut、cacheWrites、cacheReads 和
 * cost。
 *
 * @param messages 要处理的 ClineMessage 对象数组
 * @return 包含 totalTokensIn、totalTokensOut、totalCacheWrites、totalCacheReads 和 totalCost 的 ApiMetrics
 *     对象
 */
public class ApiMetricsUtils {
    private static final Logger log = LoggerFactory.getLogger(ApiMetricsUtils.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static ApiMetrics getApiMetrics(List<ClineMessage> messages) {
        ApiMetrics result =
                ApiMetrics.builder()
                        .totalTokensIn(0)
                        .totalTokensOut(0)
                        .totalCacheWrites(null)
                        .totalCacheReads(null)
                        .totalCost(0.0)
                        .build();

        if (messages == null) {
            return result;
        }

        for (ClineMessage message : messages) {
            if (message != null
                    && ClineMessageType.SAY.equals(message.getType())
                    && (ClineSay.API_REQ_STARTED.equals(message.getSay())
                            || ClineSay.DELETED_API_REQS.equals(message.getSay()))
                    && message.getText() != null) {
                try {
                    JsonNode parsedData = objectMapper.readTree(message.getText());

                    if (parsedData.has("tokensIn") && parsedData.get("tokensIn").isNumber()) {
                        result.setTotalTokensIn(
                                result.getTotalTokensIn() + parsedData.get("tokensIn").asLong());
                    }
                    if (parsedData.has("tokensOut") && parsedData.get("tokensOut").isNumber()) {
                        result.setTotalTokensOut(
                                result.getTotalTokensOut() + parsedData.get("tokensOut").asLong());
                    }
                    if (parsedData.has("cacheWrites") && parsedData.get("cacheWrites").isNumber()) {
                        long cacheWrites = parsedData.get("cacheWrites").asLong();
                        result.setTotalCacheWrites(
                                (result.getTotalCacheWrites() != null
                                                ? result.getTotalCacheWrites()
                                                : 0L)
                                        + cacheWrites);
                    }
                    if (parsedData.has("cacheReads") && parsedData.get("cacheReads").isNumber()) {
                        long cacheReads = parsedData.get("cacheReads").asLong();
                        result.setTotalCacheReads(
                                (result.getTotalCacheReads() != null
                                                ? result.getTotalCacheReads()
                                                : 0L)
                                        + cacheReads);
                    }
                    if (parsedData.has("cost") && parsedData.get("cost").isNumber()) {
                        result.setTotalCost(
                                result.getTotalCost() + parsedData.get("cost").asDouble());
                    }
                } catch (Exception e) {
                    log.error("Error parsing JSON: {}", message.getText(), e);
                }
            }
        }

        return result;
    }
}
