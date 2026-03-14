package com.hhoa.kline.core.subscription.message;

import com.hhoa.kline.core.core.shared.ExtensionState;
import com.hhoa.kline.core.subscription.MessageType;
import com.hhoa.kline.core.subscription.SubscriptionMessage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 状态消息
 *
 * <p>用于状态更新事件
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StateMessage implements SubscriptionMessage {
    private ExtensionState state;

    @Override
    public MessageType getType() {
        return MessageType.STATE;
    }
}
