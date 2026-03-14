package com.hhoa.kline.core.subscription;

import java.time.Duration;
import java.util.concurrent.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Host 请求-响应管理器
 *
 * <p>用于管理服务器通过 SSE 发送请求消息，并等待客户端通过 HTTP 响应
 *
 * <p>工作流程：
 *
 * <ol>
 *   <li>服务器调用 sendRequest() 发送请求消息（通过 SSE）
 *   <li>返回 CompletableFuture，等待客户端响应
 *   <li>客户端通过 HTTP POST 发送响应
 *   <li>调用 completeResponse() 完成对应的 Future
 * </ol>
 */
@Slf4j
public class HostRequestResponseManager {

    @Getter
    private static final HostRequestResponseManager instance = new HostRequestResponseManager();

    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

    private final SubscriptionManager subscriptionManager =
            DefaultSubscriptionManager.getInstance();
    private final ConcurrentHashMap<String, CompletableFuture<Object>> pendingRequests =
            new ConcurrentHashMap<>();

    private HostRequestResponseManager() {
        log.info("[HostRequestResponseManager] 初始化完成");
    }

    public <T> CompletableFuture<T> sendRequest(SubscriptionRequestMessage message) {
        return sendRequest(message, DEFAULT_TIMEOUT);
    }

    /**
     * 发送请求并等待响应（自定义超时）
     *
     * @param message 请求消息
     * @param timeout 超时时间
     * @param <T> 响应类型
     * @return CompletableFuture 等待响应
     */
    @SuppressWarnings("unchecked")
    public <T> CompletableFuture<T> sendRequest(
            SubscriptionRequestMessage message, Duration timeout) {
        String requestId = message.getRequestId();
        CompletableFuture<T> future = new CompletableFuture<>();

        pendingRequests.put(requestId, (CompletableFuture<Object>) future);

        subscriptionManager.send(message);

        log.debug(
                "[HostRequestResponseManager] 发送请求: requestId={}, messageType={}",
                requestId,
                message.getType());

        scheduler.schedule(
                () -> {
                    CompletableFuture<Object> removed = pendingRequests.remove(requestId);
                    if (removed != null && !removed.isDone()) {
                        removed.completeExceptionally(
                                new TimeoutException(
                                        "Request timeout after "
                                                + timeout.toMillis()
                                                + "ms: "
                                                + message.getType()));
                        log.warn(
                                "[HostRequestResponseManager] 请求超时: requestId={}, messageType={}",
                                requestId,
                                message.getType());
                    }
                },
                timeout.toMillis(),
                TimeUnit.MILLISECONDS);

        return future;
    }

    /**
     * 完成响应（由 Controller 调用）
     *
     * @param requestId 请求ID
     * @param response 响应对象
     */
    public void completeResponse(String requestId, Object response) {
        CompletableFuture<Object> future = pendingRequests.remove(requestId);
        if (future != null) {
            future.complete(response);
            log.debug(
                    "[HostRequestResponseManager] 完成响应: requestId={}, response={}",
                    requestId,
                    response != null ? response.getClass().getSimpleName() : "null");
        } else {
            log.warn("[HostRequestResponseManager] 未找到对应的请求: requestId={}", requestId);
        }
    }

    /**
     * 完成响应异常（由 Controller 调用）
     *
     * @param requestId 请求ID
     * @param error 异常
     */
    public void completeResponseExceptionally(String requestId, Throwable error) {
        CompletableFuture<Object> future = pendingRequests.remove(requestId);
        if (future != null) {
            future.completeExceptionally(error);
            log.debug(
                    "[HostRequestResponseManager] 完成响应异常: requestId={}, error={}",
                    requestId,
                    error.getMessage());
        } else {
            log.warn("[HostRequestResponseManager] 未找到对应的请求: requestId={}", requestId);
        }
    }

    public int getPendingRequestCount() {
        return pendingRequests.size();
    }

    public void clearPendingRequests() {
        int count = pendingRequests.size();
        pendingRequests.forEach(
                (requestId, future) -> {
                    if (!future.isDone()) {
                        future.completeExceptionally(
                                new IllegalStateException("Request cancelled: " + requestId));
                    }
                });
        pendingRequests.clear();
        log.info("[HostRequestResponseManager] 清理了 {} 个待处理的请求", count);
    }

    public record RequestWithId(String requestId, Object request) {}
}
