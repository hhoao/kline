package com.hhoa.kline.core.core.model.http;

import com.hhoa.kline.core.core.task.ApiChunk;
import java.util.Map;

public record HttpApiChunk(
        ChunkType type,
        String text,
        String toolName,
        Map<String, Object> toolParams,
        Integer inputTokens,
        Integer outputTokens,
        Integer cacheWriteTokens,
        Integer cacheReadTokens,
        Double totalCost,
        String reasoning,
        Object reasoningDetails,
        String thinking,
        String signature,
        String data,
        String toolId,
        String callId)
        implements ApiChunk {

    public static HttpApiChunk text(String text) {
        return new HttpApiChunk(
                ChunkType.TEXT,
                text,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }

    public static HttpApiChunk usage(Integer inputTokens, Integer outputTokens) {
        return new HttpApiChunk(
                ChunkType.USAGE,
                null,
                null,
                null,
                inputTokens,
                outputTokens,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }
}
