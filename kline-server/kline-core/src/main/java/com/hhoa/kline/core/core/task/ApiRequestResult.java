package com.hhoa.kline.core.core.task;

import com.hhoa.kline.core.core.assistant.UserContentBlock;
import com.hhoa.kline.core.core.shared.ClineApiReqInfo;
import java.util.List;
import lombok.Data;

@Data
public class ApiRequestResult {
    private String assistantMessage;
    private List<Object> reasoningDetails;
    private List<UserContentBlock> antThinkingContent;
    private ClineApiReqInfo apiReqInfo;
    private boolean didReceiveUsageddChunk;
}
