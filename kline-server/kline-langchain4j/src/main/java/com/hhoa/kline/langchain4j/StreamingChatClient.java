package com.hhoa.kline.langchain4j;

import dev.langchain4j.data.message.ChatMessage;
import java.util.List;

interface StreamingChatClient {
    void stream(List<ChatMessage> messages, StreamingChatCallback callback);
}
