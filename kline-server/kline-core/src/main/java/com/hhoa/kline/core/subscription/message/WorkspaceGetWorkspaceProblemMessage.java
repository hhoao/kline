package com.hhoa.kline.core.subscription.message;

import com.hhoa.kline.core.core.shared.proto.host.GetWorkspaceProblemsRequest;
import com.hhoa.kline.core.subscription.MessageType;
import com.hhoa.kline.core.subscription.SubscriptionRequestMessage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceGetWorkspaceProblemMessage implements SubscriptionRequestMessage {
    private GetWorkspaceProblemsRequest getWorkspaceProblemsRequest;

    @Override
    public MessageType getType() {
        return MessageType.WORKSPACE_GET_WORKSPACE_PROBLEM;
    }
}
