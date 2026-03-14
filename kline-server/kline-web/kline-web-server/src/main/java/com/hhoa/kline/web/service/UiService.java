package com.hhoa.kline.web.service;

import com.hhoa.kline.core.subscription.SubscriptionMessage;
import reactor.core.publisher.Flux;

public interface UiService {

    Flux<SubscriptionMessage> subscribe();
}
