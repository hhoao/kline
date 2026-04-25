package com.hhoa.kline.langchain4j;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.output.TokenUsage;
import java.util.List;

final class OpenAiCompatibleStreamingChatClient implements StreamingChatClient {
    private final StreamingChatModel model;

    OpenAiCompatibleStreamingChatClient(LangChain4jApiHandlerOptions options) {
        if (options == null) {
            throw new IllegalArgumentException("options must not be null");
        }

        OpenAiStreamingChatModel.OpenAiStreamingChatModelBuilder builder =
                OpenAiStreamingChatModel.builder()
                        .baseUrl(options.baseUrl())
                        .apiKey(options.apiKey())
                        .modelName(options.modelName());

        if (options.temperature() != null) {
            builder.temperature(options.temperature());
        }
        if (options.maxTokens() != null) {
            builder.maxTokens(options.maxTokens());
        }
        if (options.timeout() != null) {
            builder.timeout(options.timeout());
        }

        this.model = builder.build();
    }

    @Override
    public void stream(List<ChatMessage> messages, StreamingChatCallback callback) {
        ChatRequest chatRequest = ChatRequest.builder().messages(messages).build();
        model.chat(
                chatRequest,
                new StreamingChatResponseHandler() {
                    @Override
                    public void onPartialResponse(String partialResponse) {
                        callback.onPartialResponse(partialResponse);
                    }

                    @Override
                    public void onCompleteResponse(ChatResponse completeResponse) {
                        TokenUsage tokenUsage = completeResponse.tokenUsage();
                        Integer inputTokens = null;
                        Integer outputTokens = null;
                        if (tokenUsage != null) {
                            inputTokens = tokenUsage.inputTokenCount();
                            outputTokens = tokenUsage.outputTokenCount();
                        }
                        callback.onCompleteResponse(inputTokens, outputTokens);
                    }

                    @Override
                    public void onError(Throwable error) {
                        callback.onError(error);
                    }
                });
    }
}
