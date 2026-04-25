package com.hhoa.kline.langchain4j;

import static org.assertj.core.api.Assertions.assertThat;

import com.hhoa.kline.core.core.task.ApiChunk;
import org.junit.jupiter.api.Test;

class LangChain4jApiChunkTest {
    @Test
    void createsTextChunk() {
        ApiChunk chunk = LangChain4jApiChunk.text("hello");

        assertThat(chunk.type()).isEqualTo(ApiChunk.ChunkType.TEXT);
        assertThat(chunk.text()).isEqualTo("hello");
        assertThat(chunk.toolName()).isNull();
        assertThat(chunk.inputTokens()).isNull();
    }

    @Test
    void createsUsageChunk() {
        ApiChunk chunk = LangChain4jApiChunk.usage(11, 22);

        assertThat(chunk.type()).isEqualTo(ApiChunk.ChunkType.USAGE);
        assertThat(chunk.inputTokens()).isEqualTo(11);
        assertThat(chunk.outputTokens()).isEqualTo(22);
        assertThat(chunk.totalCost()).isNull();
    }
}
