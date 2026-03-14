package com.hhoa.kline.core.core.shared.proto.cline;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskResponse {
    @Builder.Default private String id = "";
    @Builder.Default private String task = "";
    @Builder.Default private long ts = 0L;
    @Builder.Default private boolean isFavorited = false;
    @Builder.Default private long size = 0L;
    @Builder.Default private double totalCost = 0.0;
    @Builder.Default private int tokensIn = 0;
    @Builder.Default private int tokensOut = 0;
    @Builder.Default private int cacheWrites = 0;
    @Builder.Default private int cacheReads = 0;
}
