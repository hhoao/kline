package com.hhoa.kline.langchain4j;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class LangChain4jApiHandlerOptionsTest {
    @Test
    void requiresBaseUrl() {
        assertThatThrownBy(
                        () ->
                                new LangChain4jApiHandlerOptions(
                                        "", "key", "model", null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("baseUrl must not be blank");
    }

    @Test
    void requiresApiKey() {
        assertThatThrownBy(
                        () ->
                                new LangChain4jApiHandlerOptions(
                                        "https://api.example.com/v1",
                                        " ",
                                        "model",
                                        null,
                                        null,
                                        null,
                                        null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("apiKey must not be blank");
    }

    @Test
    void requiresModelName() {
        assertThatThrownBy(
                        () ->
                                new LangChain4jApiHandlerOptions(
                                        "https://api.example.com/v1",
                                        "key",
                                        null,
                                        null,
                                        null,
                                        null,
                                        null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("modelName must not be blank");
    }

    @Test
    void defaultsProviderId() {
        LangChain4jApiHandlerOptions options =
                new LangChain4jApiHandlerOptions(
                        "https://api.example.com/v1",
                        "key",
                        "model",
                        0.2,
                        1024,
                        Duration.ofSeconds(30),
                        null);

        assertThat(options.providerId()).isEqualTo("langchain4j-openai-compatible");
    }
}
