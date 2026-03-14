package com.hhoa.kline.core.subscription.message;

import com.hhoa.kline.core.core.shared.proto.host.ShowTextDocumentRequest;
import com.hhoa.kline.core.subscription.MessageType;
import com.hhoa.kline.core.subscription.SubscriptionRequestMessage;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WindowShowTextDocumentMessage implements SubscriptionRequestMessage {
    private ShowTextDocumentRequest request;

    @Override
    public MessageType getType() {
        return MessageType.WINDOW_SHOW_TEXT_DOCUMENT;
    }
}
