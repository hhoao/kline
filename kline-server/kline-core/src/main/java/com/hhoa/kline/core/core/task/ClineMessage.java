package com.hhoa.kline.core.core.task;

import com.hhoa.kline.core.core.shared.ClineApiReqCancelReason;
import com.hhoa.kline.core.core.shared.ClineAsk;
import com.hhoa.kline.core.core.shared.ClineMessageFormat;
import com.hhoa.kline.core.core.shared.ClineMessageType;
import com.hhoa.kline.core.core.shared.ClineSay;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClineMessage {
    private Long ts;
    private String taskId;
    private ClineMessageType type;
    private ClineAsk ask;
    private ClineSay say;
    private String text;
    private ClineApiReqCancelReason reasoning;
    private List<String> images;
    private List<String> files;
    private Boolean partial;
    private ClineMessageFormat format;
    private Boolean commandCompleted;
    private String lastCheckpointHash;
    private Boolean isCheckpointCheckedOut;
    private Boolean isOperationOutsideWorkspace;
    private Integer conversationHistoryIndex;
    private int[] conversationHistoryDeletedRange; // [number, number]

    public ClineMessage(String text, Long ts) {
        this.text = text;
        this.ts = ts;
    }
}
