package com.hhoa.kline.core.core.model.http;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.hhoa.kline.core.core.assistant.AssistantMessage;
import com.hhoa.kline.core.core.assistant.TextContentBlock;
import com.hhoa.kline.core.core.assistant.UserMessage;
import com.hhoa.kline.core.core.task.ApiChunk;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

class HttpApiHandlerTest {
    @Test
    void streamsApiChunks() {
        FakeHttpChatClient fakeClient =
                new FakeHttpChatClient(
                        Flux.just(
                                HttpChatChunk.text("你", "{}"),
                                HttpChatChunk.text("好", "{}"),
                                HttpChatChunk.usage(3, 4, "{}"),
                                HttpChatChunk.done()));
        HttpApiHandler handler = new HttpApiHandler(options(null), fakeClient);

        StepVerifier.create(
                        handler.createMessageStream(
                                "system", List.of(user("hello"), assistant("hi"))))
                .assertNext(
                        chunk -> {
                            assertEquals(ApiChunk.ChunkType.TEXT, chunk.type());
                            assertEquals("你", chunk.text());
                        })
                .assertNext(
                        chunk -> {
                            assertEquals(ApiChunk.ChunkType.TEXT, chunk.type());
                            assertEquals("好", chunk.text());
                        })
                .assertNext(
                        chunk -> {
                            assertEquals(ApiChunk.ChunkType.USAGE, chunk.type());
                            assertEquals(3, chunk.inputTokens());
                            assertEquals(4, chunk.outputTokens());
                        })
                .verifyComplete();

        HttpChatRequest request = fakeClient.lastRequest();
        assertEquals("https://api.example.com", request.baseUrl());
        assertEquals("test-key", request.apiKey());
        assertEquals("test-model", request.model());
        assertEquals(0.2, request.temperature());
        assertEquals(32, request.maxTokens());
        assertEquals(Duration.ofSeconds(5), request.timeout());
        assertEquals(
                List.of(
                        new HttpChatMessage("system", "system"),
                        new HttpChatMessage("user", "hello"),
                        new HttpChatMessage("assistant", "hi")),
                request.messages());
    }

    @Test
    void exposesModelAndProvider() {
        HttpApiHandler handler = new HttpApiHandler(options("openai-compatible-http"));

        assertEquals("test-model", handler.getModelId());
        assertEquals("openai-compatible-http", handler.getProviderId());
        assertEquals(null, handler.getLastRequestId());
    }

    private HttpApiHandler.Options options(String providerId) {
        return new HttpApiHandler.Options(
                "https://api.example.com",
                "test-key",
                "test-model",
                0.2,
                32,
                Duration.ofSeconds(5),
                providerId);
    }

    private UserMessage user(String text) {
        return UserMessage.builder().content(List.of(new TextContentBlock(text))).build();
    }

    private AssistantMessage assistant(String text) {
        return AssistantMessage.builder().content(List.of(new TextContentBlock(text))).build();
    }

    private static class FakeHttpChatClient implements HttpChatClient {
        private final Flux<HttpChatChunk> stream;
        private HttpChatRequest lastRequest;

        private FakeHttpChatClient(Flux<HttpChatChunk> stream) {
            this.stream = stream;
        }

        @Override
        public HttpChatResponse complete(HttpChatRequest request) {
            throw new UnsupportedOperationException("complete is not used by HttpApiHandler");
        }

        @Override
        public Flux<HttpChatChunk> stream(HttpChatRequest request) {
            lastRequest = request;
            return stream;
        }

        private HttpChatRequest lastRequest() {
            return lastRequest;
        }
    }
}
