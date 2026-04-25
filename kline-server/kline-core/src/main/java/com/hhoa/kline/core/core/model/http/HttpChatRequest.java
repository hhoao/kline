package com.hhoa.kline.core.core.model.http;

import java.time.Duration;
import java.util.List;

public record HttpChatRequest(
        String baseUrl,
        String apiKey,
        String model,
        List<HttpChatMessage> messages,
        Double temperature,
        Integer maxTokens,
        Duration timeout) {
    public HttpChatRequest {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("baseUrl must not be blank");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("apiKey must not be blank");
        }
        if (model == null || model.isBlank()) {
            throw new IllegalArgumentException("model must not be blank");
        }
        if (messages == null || messages.isEmpty()) {
            throw new IllegalArgumentException("messages must not be empty");
        }

        baseUrl = removeTrailingSlashes(baseUrl);
        messages = List.copyOf(messages);
    }

    private static String removeTrailingSlashes(String value) {
        int end = value.length();
        while (end > 0 && value.charAt(end - 1) == '/') {
            end--;
        }
        return value.substring(0, end);
    }
}
