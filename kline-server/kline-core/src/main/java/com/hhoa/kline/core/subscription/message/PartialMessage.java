package com.hhoa.kline.core.subscription.message;

import com.hhoa.kline.core.core.shared.ClineApiReqCancelReason;
import com.hhoa.kline.core.core.shared.ClineAsk;
import com.hhoa.kline.core.core.shared.ClineMessageFormat;
import com.hhoa.kline.core.core.shared.ClineMessageType;
import com.hhoa.kline.core.core.shared.ClineSay;
import com.hhoa.kline.core.subscription.MessageType;
import com.hhoa.kline.core.subscription.SubscriptionMessage;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 部分消息
 *
 * <p>用于部分消息更新事件
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PartialMessage implements SubscriptionMessage {
    private Long ts;
    private String taskId;
    private ClineMessageType clineMessageType;
    private ClineAsk ask;
    private ClineSay say;
    private String incrementContent;
    private ClineMessageFormat format;
    private ClineApiReqCancelReason reasoning;
    private List<String> images;
    private List<String> files;
    private Boolean commandCompleted;
    private Boolean isUpdatingPreviousPartial;

    @Override
    public MessageType getType() {
        return MessageType.PARTIAL_MESSAGE;
    }
}
