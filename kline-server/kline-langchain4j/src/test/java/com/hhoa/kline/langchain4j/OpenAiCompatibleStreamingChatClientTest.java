package com.hhoa.kline.langchain4j;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class OpenAiCompatibleStreamingChatClientTest {
    @Test
    void requiresOptions() {
        assertThatThrownBy(() -> new OpenAiCompatibleStreamingChatClient(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("options must not be null");
    }
}
