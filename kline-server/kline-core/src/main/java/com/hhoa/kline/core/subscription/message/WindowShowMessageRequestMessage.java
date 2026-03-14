package com.hhoa.kline.core.subscription.message;

import com.hhoa.kline.core.core.shared.proto.host.ShowMessageRequest;
import com.hhoa.kline.core.subscription.MessageType;
import com.hhoa.kline.core.subscription.SubscriptionRequestMessage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Window ShowMessage 请求消息
 *
 * <p>用于显示消息对话框的请求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WindowShowMessageRequestMessage implements SubscriptionRequestMessage {
    private ShowMessageRequest request;

    @Override
    public MessageType getType() {
        return MessageType.WINDOW_SHOW_MESSAGE;
    }
}
