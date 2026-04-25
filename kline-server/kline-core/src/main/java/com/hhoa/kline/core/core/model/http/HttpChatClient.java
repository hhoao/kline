package com.hhoa.kline.core.core.model.http;

import reactor.core.publisher.Flux;

public interface HttpChatClient {
    HttpChatResponse complete(HttpChatRequest request);

    Flux<HttpChatChunk> stream(HttpChatRequest request);
}
