package com.hhoa.kline.core.subscription.message;

import com.hhoa.kline.core.core.shared.proto.host.OpenInFileExplorerPanelRequest;
import com.hhoa.kline.core.subscription.MessageType;
import com.hhoa.kline.core.subscription.SubscriptionRequestMessage;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WorkspaceOpenInFileExplorerPanelRequestMessage implements SubscriptionRequestMessage {
    private OpenInFileExplorerPanelRequest request;

    @Override
    public MessageType getType() {
        return MessageType.WORKSPACE_OPEN_IN_FILE_EXPLORER_PANEL;
    }
}
