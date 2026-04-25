package com.hhoa.kline.langchain4j;

import com.hhoa.kline.core.core.assistant.MessageParam;
import com.hhoa.kline.core.core.task.ApiChunk;
import com.hhoa.kline.core.core.task.ApiHandler;
import dev.langchain4j.data.message.ChatMessage;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import reactor.core.publisher.Flux;

public final class LangChain4jApiHandler implements ApiHandler {
    private final LangChain4jApiHandlerOptions options;
    private final StreamingChatClient streamingChatClient;

    public LangChain4jApiHandler(LangChain4jApiHandlerOptions options) {
        this(options, new OpenAiCompatibleStreamingChatClient(options));
    }

    LangChain4jApiHandler(
            LangChain4jApiHandlerOptions options, StreamingChatClient streamingChatClient) {
        if (options == null) {
            throw new IllegalArgumentException("options must not be null");
        }
        if (streamingChatClient == null) {
            throw new IllegalArgumentException("streamingChatClient must not be null");
        }
        this.options = options;
        this.streamingChatClient = streamingChatClient;
    }

    @Override
    public String getLastRequestId() {
        return null;
    }

    @Override
    public Flux<ApiChunk> createMessageStream(
            String systemPrompt, List<MessageParam> conversationHistory) {
        List<ChatMessage> messages =
                LangChain4jMessageConverter.convert(systemPrompt, conversationHistory);

        return Flux.create(
                sink -> {
                    AtomicBoolean active = new AtomicBoolean(true);
                    sink.onCancel(() -> active.set(false));
                    sink.onDispose(() -> active.set(false));

                    try {
                        streamingChatClient.stream(
                                messages,
                                new StreamingChatCallback() {
                                    @Override
                                    public void onPartialResponse(String partialResponse) {
                                        if (active.get()
                                                && partialResponse != null
                                                && !partialResponse.isEmpty()) {
                                            sink.next(LangChain4jApiChunk.text(partialResponse));
                                        }
                                    }

                                    @Override
                                    public void onCompleteResponse(
                                            Integer inputTokens, Integer outputTokens) {
                                        if (active.compareAndSet(true, false)) {
                                            if (inputTokens != null || outputTokens != null) {
                                                sink.next(
                                                        LangChain4jApiChunk.usage(
                                                                inputTokens, outputTokens));
                                            }
                                            sink.complete();
                                        }
                                    }

                                    @Override
                                    public void onError(Throwable error) {
                                        if (active.compareAndSet(true, false)) {
                                            sink.error(error);
                                        }
                                    }
                                });
                    } catch (RuntimeException error) {
                        if (active.compareAndSet(true, false)) {
                            sink.error(error);
                        }
                    }
                });
    }

    @Override
    public String getModelId() {
        return options.modelName();
    }

    @Override
    public String getProviderId() {
        return options.providerId();
    }
}
