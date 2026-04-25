package com.hhoa.kline.core.core.model.http;

public class HttpChatException extends RuntimeException {
    private static final long serialVersionUID = 3426166952367487196L;

    private final int statusCode;
    private final String responseBody;

    public HttpChatException(int statusCode, String responseBody) {
        super("HTTP model request failed with status %d: %s".formatted(statusCode, responseBody));
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    public int statusCode() {
        return statusCode;
    }

    public String responseBody() {
        return responseBody;
    }
}
