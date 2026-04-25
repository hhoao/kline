package com.hhoa.kline.core.core.model.http;

public record HttpChatResponse(
        String text, Integer inputTokens, Integer outputTokens, String raw) {}
