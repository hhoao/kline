package com.hhoa.kline.core.subscription;

import java.util.UUID;

/**
 * 订阅请求消息接口
 *
 * <p>扩展 SubscriptionMessage，用于需要等待客户端响应的请求消息
 */
public interface SubscriptionRequestMessage extends SubscriptionMessage {
    default String getRequestId() {
        return generateRequestId(getType());
    }

    private String generateRequestId(MessageType messageType) {
        return messageType.name() + "-" + UUID.randomUUID().toString().replace("-", "");
    }
}
