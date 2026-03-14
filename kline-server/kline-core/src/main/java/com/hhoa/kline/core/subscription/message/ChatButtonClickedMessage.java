package com.hhoa.kline.core.subscription.message;

import com.hhoa.kline.core.subscription.MessageType;
import com.hhoa.kline.core.subscription.SubscriptionMessage;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 聊天按钮点击消息
 *
 * <p>用于聊天按钮点击事件
 */
@Data
@NoArgsConstructor
public class ChatButtonClickedMessage implements SubscriptionMessage {

    @Override
    public MessageType getType() {
        return MessageType.CHAT_BUTTON_CLICKED;
    }
}
