package com.hhoa.kline.core.subscription.message;

import com.hhoa.kline.core.core.shared.proto.host.OpenTerminalRequest;
import com.hhoa.kline.core.subscription.MessageType;
import com.hhoa.kline.core.subscription.SubscriptionRequestMessage;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WorkspaceOpenTerminalMessage implements SubscriptionRequestMessage {
    private OpenTerminalRequest request;

    @Override
    public MessageType getType() {
        return MessageType.WORKSPACE_OPEN_TERMINAL;
    }
}
