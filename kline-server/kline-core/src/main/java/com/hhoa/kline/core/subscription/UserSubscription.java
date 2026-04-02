package com.hhoa.kline.core.subscription;

import java.util.concurrent.atomic.AtomicInteger;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import reactor.core.Scannable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * 用户订阅
 *
 * <p>管理单个用户的订阅流
 */
@Data
@Slf4j
public class UserSubscription implements Subscription {
    private final String userId;
    private final Sinks.Many<SubscriptionMessage> sink;
    private final Flux<SubscriptionMessage> flux;
    private final AtomicInteger subscriberCount = new AtomicInteger(0);

    public UserSubscription(String userId) {
        this.userId = userId;
        this.sink = Sinks.many().multicast().onBackpressureBuffer();

        this.flux = this.sink.asFlux();
    }

    @Override
    public void send(SubscriptionMessage message) {
        if (message == null) {
            log.warn("[SubscriptionManager] 用户 {} 消息为 null，跳过发送", userId);
            return;
        }

        sink.emitNext(
                message,
                (signalType, emitResult) -> {
                    if (emitResult == Sinks.EmitResult.FAIL_TERMINATED) {
                        log.error("[SubscriptionManager] 用户 {} Sink已终止，无法发送事件", userId);
                        return false;
                    }

                    if (emitResult == Sinks.EmitResult.FAIL_OVERFLOW) {
                        log.warn("[SubscriptionManager] 用户 {} Sink溢出，等待后重试", userId);
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            log.error("[SubscriptionManager] 用户 {} 重试被中断", userId, e);
                            return false;
                        }
                        return true;
                    }

                    if (emitResult == Sinks.EmitResult.FAIL_NON_SERIALIZED) {
                        log.debug("[SubscriptionManager] 用户 {} 并发发送冲突，等待后重试", userId);
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            log.error("[SubscriptionManager] 用户 {} 重试被中断", userId, e);
                            return false;
                        }
                        return true;
                    }

                    log.warn("[SubscriptionManager] 用户 {} 发送失败: {}", userId, emitResult);
                    return false;
                });
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
