package com.hhoa.kline.langchain4j;

public interface StreamingChatCallback {
    void onPartialResponse(String partialResponse);

    void onCompleteResponse(Integer inputTokens, Integer outputTokens);

    void onError(Throwable error);
}
