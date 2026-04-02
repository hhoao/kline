package com.hhoa.kline.core.subscription;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

/**
 * 统一的订阅管理器
 *
 * <p>支持按用户ID管理 Flux，每个用户对应一个 Flux，可以有多个 handler 处理消息
 */
@Slf4j
public class DefaultSubscriptionManager implements SubscriptionManager {

    @Getter private static final SubscriptionManager instance = new DefaultSubscriptionManager();

    private static final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(30);

    private final ConcurrentHashMap<String, UserSubscription> userSubscriptions =
            new ConcurrentHashMap<>();

    private DefaultSubscriptionManager() {
        log.info("[SubscriptionManager] 初始化完成");
    }

    @Override
    public MessageSender createMessageSender(Long taskManagerId) {
        if (taskManagerId == null) {
            throw new IllegalArgumentException("taskManagerId == null");
        }
        return (message) -> {
            if (message == null) {
                log.warn("[TaskContextMessageSender] 消息为 null，跳过发送");
                return;
            }

            UserSubscription userSubscription =
                    userSubscriptions.get(String.valueOf(taskManagerId));
            if (userSubscription == null) {
                log.warn("[TaskContextMessageSender] 用户订阅不存在，跳过发送");
                return;
            }

            userSubscription.send(message);
        };
    }

    private UserSubscription getOrCreateUserSubscription(String userId) {
        UserSubscription existing = userSubscriptions.get(userId);
        if (existing != null && existing.isTerminated()) {
            userSubscriptions.remove(userId, existing);
        }
        return userSubscriptions.computeIfAbsent(userId, UserSubscription::new);
    }

    @Override
    public Flux<SubscriptionMessage> subscribe(String userId) {
        return subscribe(userId, DEFAULT_TIMEOUT);
    }

    @Override
    public Flux<SubscriptionMessage> subscribe(String userId, Duration timeout) {
        UserSubscription userSubscription = getOrCreateUserSubscription(userId);
        return userSubscription
                .getFlux()
                .cast(SubscriptionMessage.class)
                .timeout(timeout)
                .doOnTerminate(() -> log.info("[SubscriptionManager] 用户 {} 订阅流已终止", userId))
                .doOnSubscribe(
                        subscription -> {
                            int count = userSubscription.incrementSubscriberCount();
                            log.info(
                                    "[SubscriptionManager] 用户 {} 新订阅者已连接，当前订阅者数: {}",
                                    userId,
                                    count);
                        })
                .doOnCancel(() -> log.info("[SubscriptionManager] 用户 {} 订阅者已取消订阅", userId))
                .doOnError(
                        TimeoutException.class,
                        error -> {
                            log.warn(
                                    "[SubscriptionManager] 用户 {} 订阅超时，当前订阅者数: {}",
                                    userId,
                                    userSubscription.getSubscriberCount());
                        })
                .onErrorComplete(TimeoutException.class)
                .doOnError(
                        error ->
                                log.error(
                                        "[SubscriptionManager] 用户 {} 订阅流发生错误，当前订阅者数: {}",
                                        userId,
                                        userSubscription.getSubscriberCount(),
                                        error))
                .doFinally(
                        signalType -> {
                            int count = userSubscription.decrementSubscriberCount();
                            if (count == 0) {
                                userSubscriptions.remove(userId, userSubscription);
                            }
                            log.debug(
                                    "[SubscriptionManager] 用户 {} 订阅流结束: {}，当前订阅者数: {}",
                                    userId,
                                    signalType,
                                    count);
                        });
    }

    @Override
    public int getSubscriberCount(String userId) {
        UserSubscription userSubscription = userSubscriptions.get(userId);
        return userSubscription != null ? userSubscription.getSubscriberCount() : 0;
    }

    @Override
    public void shutdown(String userId) {
        UserSubscription userSubscription = userSubscriptions.remove(userId);
        if (userSubscription != null) {
            log.info(
                    "[SubscriptionManager] 用户 {} 正在关闭，当前订阅者数: {}",
                    userId,
                    userSubscription.getSubscriberCount());
            userSubscription.shutdown();
        }
    }

    @Override
    public void shutdownAll() {
        log.info("[SubscriptionManager] 正在关闭所有订阅，用户数: {}", userSubscriptions.size());
        userSubscriptions.forEach(
                (userId, subscription) -> {
                    try {
                        subscription.shutdown();
                    } catch (Exception e) {
                        log.error("[SubscriptionManager] 关闭用户 {} 订阅失败", userId, e);
                    }
                });
        userSubscriptions.clear();
    }

    @Override
    public boolean isTerminated(String userId) {
        UserSubscription userSubscription = userSubscriptions.get(userId);
        if (userSubscription == null) {
            return true;
        }
        return userSubscription.isTerminated();
    }
}
