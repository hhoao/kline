package com.hhoa.kline.core.core.model.http;

import com.hhoa.kline.core.core.assistant.AssistantMessage;
import com.hhoa.kline.core.core.assistant.MessageParam;
import com.hhoa.kline.core.core.assistant.TextContentBlock;
import com.hhoa.kline.core.core.assistant.UserContentBlock;
import com.hhoa.kline.core.core.assistant.UserMessage;
import com.hhoa.kline.core.core.task.ApiChunk;
import com.hhoa.kline.core.core.task.ApiHandler;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import reactor.core.publisher.Flux;

public class HttpApiHandler implements ApiHandler {
    public static final String DEFAULT_PROVIDER_ID = "openai-compatible-http";

    private final Options options;
    private final HttpChatClient httpChatClient;

    public HttpApiHandler(Options options) {
        this(options, new OpenAiCompatibleHttpChatClient());
    }

    public HttpApiHandler(Options options, HttpChatClient httpChatClient) {
        this.options = Objects.requireNonNull(options, "options must not be null");
        this.httpChatClient =
                Objects.requireNonNull(httpChatClient, "httpChatClient must not be null");
    }

    @Override
    public String getLastRequestId() {
        return null;
    }

    @Override
    public Flux<ApiChunk> createMessageStream(
            String systemPrompt, List<MessageParam> conversationHistory) {
        HttpChatRequest request =
                new HttpChatRequest(
                        options.baseUrl(),
                        options.apiKey(),
                        options.model(),
                        messages(systemPrompt, conversationHistory),
                        options.temperature(),
                        options.maxTokens(),
                        options.timeout());

        return httpChatClient.stream(request).handle(this::emitApiChunk);
    }

    @Override
    public String getModelId() {
        return options.model();
    }

    @Override
    public String getProviderId() {
        return options.providerId();
    }

    private List<HttpChatMessage> messages(
            String systemPrompt, List<MessageParam> conversationHistory) {
        List<HttpChatMessage> messages = new ArrayList<>();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            messages.add(new HttpChatMessage("system", systemPrompt));
        }
        if (conversationHistory == null) {
            return messages;
        }

        for (MessageParam message : conversationHistory) {
            HttpChatMessage httpMessage = httpChatMessage(message);
            if (httpMessage != null) {
                messages.add(httpMessage);
            }
        }
        return messages;
    }

    private HttpChatMessage httpChatMessage(MessageParam message) {
        if (message instanceof UserMessage) {
            return textMessage("user", message.getContent());
        }
        if (message instanceof AssistantMessage) {
            return textMessage("assistant", message.getContent());
        }
        return null;
    }

    private HttpChatMessage textMessage(String role, List<UserContentBlock> content) {
        if (content == null || content.isEmpty()) {
            return null;
        }

        String text =
                content.stream()
                        .filter(TextContentBlock.class::isInstance)
                        .map(TextContentBlock.class::cast)
                        .map(TextContentBlock::getText)
                        .filter(value -> value != null && !value.isBlank())
                        .collect(Collectors.joining("\n"));
        if (text.isEmpty()) {
            return null;
        }
        return new HttpChatMessage(role, text);
    }

    private void emitApiChunk(
            HttpChatChunk chunk, reactor.core.publisher.SynchronousSink<ApiChunk> sink) {
        if (chunk.type() == HttpChatChunk.Type.TEXT) {
            sink.next(HttpApiChunk.text(chunk.text()));
        } else if (chunk.type() == HttpChatChunk.Type.USAGE) {
            sink.next(HttpApiChunk.usage(chunk.inputTokens(), chunk.outputTokens()));
        }
    }

    public record Options(
            String baseUrl,
            String apiKey,
            String model,
            Double temperature,
            Integer maxTokens,
            Duration timeout,
            String providerId) {
        public Options {
            if (baseUrl == null || baseUrl.isBlank()) {
                throw new IllegalArgumentException("baseUrl must not be blank");
            }
            if (apiKey == null || apiKey.isBlank()) {
                throw new IllegalArgumentException("apiKey must not be blank");
            }
            if (model == null || model.isBlank()) {
                throw new IllegalArgumentException("model must not be blank");
            }
            if (providerId == null || providerId.isBlank()) {
                providerId = DEFAULT_PROVIDER_ID;
            }
        }
    }
}
