package com.hhoa.kline.core.core.task;

import com.hhoa.kline.core.core.shared.ClineAsk;
import lombok.Data;

@Data
public class AskPending {
    private String pendingId;
    private ClineAsk askType;
}
