package com.hhoa.kline.langchain4j;

import static org.assertj.core.api.Assertions.assertThat;

import com.hhoa.kline.core.core.assistant.MessageParam;
import com.hhoa.kline.core.core.assistant.TextContentBlock;
import com.hhoa.kline.core.core.assistant.UserMessage;
import com.hhoa.kline.core.core.task.ApiChunk;
import dev.langchain4j.data.message.ChatMessage;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

class LangChain4jApiHandlerTest {
    @Test
    void exposesModelAndProvider() {
        LangChain4jApiHandlerOptions options = options();
        LangChain4jApiHandler handler =
                new LangChain4jApiHandler(options, new FakeStreamingChatClient());

        assertThat(handler.getModelId()).isEqualTo("test-model");
        assertThat(handler.getProviderId()).isEqualTo("langchain4j-openai-compatible");
    }

    @Test
    void streamsTextAndUsageChunks() {
        FakeStreamingChatClient client = new FakeStreamingChatClient();
        client.partialResponses = List.of("你", "好");
        client.inputTokens = 3;
        client.outputTokens = 5;

        LangChain4jApiHandler handler = new LangChain4jApiHandler(options(), client);

        StepVerifier.create(handler.createMessageStream("system", List.of(userText("hello"))))
                .assertNext(chunk -> assertText(chunk, "你"))
                .assertNext(chunk -> assertText(chunk, "好"))
                .assertNext(
                        chunk -> {
                            assertThat(chunk.type()).isEqualTo(ApiChunk.ChunkType.USAGE);
                            assertThat(chunk.inputTokens()).isEqualTo(3);
                            assertThat(chunk.outputTokens()).isEqualTo(5);
                        })
                .verifyComplete();

        assertThat(client.lastMessages)
                .hasValueSatisfying(messages -> assertThat(messages).hasSize(2));
    }

    @Test
    void propagatesStreamingErrors() {
        FakeStreamingChatClient client = new FakeStreamingChatClient();
        client.error = new IllegalStateException("model failed");

        LangChain4jApiHandler handler = new LangChain4jApiHandler(options(), client);

        StepVerifier.create(handler.createMessageStream("system", List.of(userText("hello"))))
                .expectErrorSatisfies(
                        error ->
                                assertThat(error)
                                        .isInstanceOf(IllegalStateException.class)
                                        .hasMessage("model failed"))
                .verify();
    }

    @Test
    void ignoresCallbacksAfterCancellation() {
        ManualStreamingChatClient client = new ManualStreamingChatClient();
        LangChain4jApiHandler handler = new LangChain4jApiHandler(options(), client);

        StepVerifier.create(handler.createMessageStream("system", List.of(userText("hello"))), 1)
                .then(() -> client.callback.onPartialResponse("first"))
                .assertNext(chunk -> assertText(chunk, "first"))
                .thenCancel()
                .verify();

        client.callback.onPartialResponse("second");
        client.callback.onCompleteResponse(1, 1);
    }

    private static LangChain4jApiHandlerOptions options() {
        return new LangChain4jApiHandlerOptions(
                "https://api.example.com/v1",
                "key",
                "test-model",
                null,
                null,
                Duration.ofSeconds(10),
                null);
    }

    private static MessageParam userText(String text) {
        return UserMessage.builder().content(List.of(new TextContentBlock(text))).build();
    }

    private static void assertText(ApiChunk chunk, String text) {
        assertThat(chunk.type()).isEqualTo(ApiChunk.ChunkType.TEXT);
        assertThat(chunk.text()).isEqualTo(text);
    }

    private static final class FakeStreamingChatClient implements StreamingChatClient {
        private List<String> partialResponses = List.of();
        private Integer inputTokens;
        private Integer outputTokens;
        private Throwable error;
        private final AtomicReference<List<ChatMessage>> lastMessages = new AtomicReference<>();

        @Override
        public void stream(List<ChatMessage> messages, StreamingChatCallback callback) {
            lastMessages.set(messages);
            if (error != null) {
                callback.onError(error);
                return;
            }
            for (String partialResponse : partialResponses) {
                callback.onPartialResponse(partialResponse);
            }
            callback.onCompleteResponse(inputTokens, outputTokens);
        }
    }

    private static final class ManualStreamingChatClient implements StreamingChatClient {
        private StreamingChatCallback callback;

        @Override
        public void stream(List<ChatMessage> messages, StreamingChatCallback callback) {
            this.callback = callback;
        }
    }
}
