package com.hhoa.kline.core.core.model.http;

public record HttpChatChunk(
        Type type, String text, Integer inputTokens, Integer outputTokens, String raw) {
    public enum Type {
        TEXT,
        USAGE,
        DONE
    }

    public static HttpChatChunk text(String text, String raw) {
        return new HttpChatChunk(Type.TEXT, text, null, null, raw);
    }

    public static HttpChatChunk usage(Integer inputTokens, Integer outputTokens, String raw) {
        return new HttpChatChunk(Type.USAGE, null, inputTokens, outputTokens, raw);
    }

    public static HttpChatChunk done() {
        return new HttpChatChunk(Type.DONE, null, null, null, null);
    }
}
