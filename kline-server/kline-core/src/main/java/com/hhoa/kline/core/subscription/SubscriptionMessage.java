package com.hhoa.kline.core.subscription;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 订阅消息接口
 *
 * <p>所有订阅消息类型都需要实现此接口，用于类型识别和统一处理
 */
public interface SubscriptionMessage {
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    MessageType getType();
}
