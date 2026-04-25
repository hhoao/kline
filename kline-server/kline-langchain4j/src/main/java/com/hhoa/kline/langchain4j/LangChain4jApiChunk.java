package com.hhoa.kline.langchain4j;

import com.hhoa.kline.core.core.task.ApiChunk;
import java.util.Map;

public record LangChain4jApiChunk(
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
    public static LangChain4jApiChunk text(String text) {
        return new LangChain4jApiChunk(
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

    public static LangChain4jApiChunk usage(Integer inputTokens, Integer outputTokens) {
        return new LangChain4jApiChunk(
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
