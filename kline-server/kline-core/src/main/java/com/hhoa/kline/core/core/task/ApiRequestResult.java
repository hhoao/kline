package com.hhoa.kline.core.core.task;

import com.hhoa.kline.core.core.assistant.UserContentBlock;
import com.hhoa.kline.core.core.shared.ClineApiReqInfo;
import java.util.List;
import lombok.Data;

@Data
public class ApiRequestResult {
    public ApiRequestResult(ExistState existState) {
        this.existState = existState;
    }

    private String assistantMessage;
    private List<Object> reasoningDetails;
    private List<UserContentBlock> antThinkingContent;
    private ClineApiReqInfo apiReqInfo;
    private boolean didReceiveUsageChunk;
    private ExistState existState;
}
