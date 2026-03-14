package com.hhoa.kline.core.subscription;

import java.time.Duration;
import reactor.core.publisher.Flux;

public interface SubscriptionManager {

    void send(SubscriptionMessage message);

    UserSubscription getOrCreateUserSubscription(String userId);

    Flux<SubscriptionMessage> subscribe(String userId);

    Flux<SubscriptionMessage> subscribe(String userId, Duration timeout);

    int getSubscriberCount(String userId);

    void shutdown(String userId);

    void shutdownAll();

    boolean isTerminated(String userId);
}
