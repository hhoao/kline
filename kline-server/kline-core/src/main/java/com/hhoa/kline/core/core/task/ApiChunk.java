package com.hhoa.kline.core.core.task;

import java.util.Map;
import lombok.Getter;

public interface ApiChunk {
    ChunkType type();

    String text();

    String toolName();

    Map<String, Object> toolParams();

    Integer inputTokens();

    Integer outputTokens();

    Integer cacheWriteTokens();

    Integer cacheReadTokens();

    Double totalCost();

    String reasoning();

    Object reasoningDetails();

    String thinking();

    String signature();

    String data();

    @Getter
    enum ChunkType {
        TEXT("text"),
        TOOL_USE("tool_use"),
        USAGE("usage"),
        REASONING("reasoning"),
        REASONING_DETAILS("reasoning_details"),
        ANT_THINKING("ant_thinking"),
        ANT_REDACTED_THINKING("ant_redacted_thinking");

        private final String value;

        ChunkType(String value) {
            this.value = value;
        }
    }
}
