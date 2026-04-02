package com.hhoa.kline.core.core.controller.testsupport;

import com.hhoa.kline.core.core.task.ApiChunk;
import java.util.Map;

public record SimpleTestApiChunk(
        ApiChunk.ChunkType type,
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

    public static SimpleTestApiChunk textChunk(String text) {
        return new SimpleTestApiChunk(
                ApiChunk.ChunkType.TEXT,
                text,
                null,
                Map.of(),
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

    public static SimpleTestApiChunk usageChunk(int inputTokens, int outputTokens) {
        return new SimpleTestApiChunk(
                ApiChunk.ChunkType.USAGE,
                null,
                null,
                Map.of(),
                inputTokens,
                outputTokens,
                0,
                0,
                0.0,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }
}
