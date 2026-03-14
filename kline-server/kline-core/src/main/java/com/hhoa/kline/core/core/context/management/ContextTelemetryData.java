package com.hhoa.kline.core.core.context.management;

import lombok.Data;

@Data
public class ContextTelemetryData {
    private final int tokensUsed;
    private final int maxContextWindow;

    public ContextTelemetryData(int tokensUsed, int maxContextWindow) {
        this.tokensUsed = tokensUsed;
        this.maxContextWindow = maxContextWindow;
    }
}
