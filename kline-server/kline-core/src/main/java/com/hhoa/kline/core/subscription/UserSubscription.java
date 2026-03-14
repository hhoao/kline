package com.hhoa.kline.core.subscription;

import java.util.concurrent.atomic.AtomicInteger;
import lombok.Data;
import reactor.core.Scannable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * 用户订阅
 *
 * <p>管理单个用户的订阅流
 */
@Data
public class UserSubscription {
    private final String userId;
    private final Sinks.Many<Object> sink;
    private final Flux<Object> flux;
    private final AtomicInteger subscriberCount = new AtomicInteger(0);

    public UserSubscription(String userId) {
        this.userId = userId;
        this.sink = Sinks.many().multicast().onBackpressureBuffer();

        this.flux = this.sink.asFlux();
    }

    public int incrementSubscriberCount() {
        return subscriberCount.incrementAndGet();
    }

    public int decrementSubscriberCount() {
        return subscriberCount.decrementAndGet();
    }

    public int getSubscriberCount() {
        return subscriberCount.get();
    }

    public void shutdown() {
        sink.tryEmitComplete();
    }

    public boolean isTerminated() {
        return sink.currentSubscriberCount() == 0
                && sink.scanOrDefault(Scannable.Attr.TERMINATED, false);
    }
}
