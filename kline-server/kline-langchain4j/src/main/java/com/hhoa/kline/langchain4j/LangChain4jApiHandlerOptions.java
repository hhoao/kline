package com.hhoa.kline.langchain4j;

import java.time.Duration;

public record LangChain4jApiHandlerOptions(
        String baseUrl,
        String apiKey,
        String modelName,
        Double temperature,
        Integer maxTokens,
        Duration timeout,
        String providerId) {
    public static final String DEFAULT_PROVIDER_ID = "langchain4j-openai-compatible";

    public LangChain4jApiHandlerOptions {
        baseUrl = requireNotBlank(baseUrl, "baseUrl");
        apiKey = requireNotBlank(apiKey, "apiKey");
        modelName = requireNotBlank(modelName, "modelName");
        providerId = isBlank(providerId) ? DEFAULT_PROVIDER_ID : providerId.trim();
    }

    private static String requireNotBlank(String value, String fieldName) {
        if (isBlank(value)) {
            throw new IllegalArgumentException("%s must not be blank".formatted(fieldName));
        }
        return value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
