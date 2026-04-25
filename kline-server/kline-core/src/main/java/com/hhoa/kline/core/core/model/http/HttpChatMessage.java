package com.hhoa.kline.core.core.model.http;

public record HttpChatMessage(String role, String content) {
    public HttpChatMessage {
        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException("role must not be blank");
        }
        if (content == null) {
            content = "";
        }
    }
}
