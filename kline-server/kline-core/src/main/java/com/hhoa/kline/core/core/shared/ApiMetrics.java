package com.hhoa.kline.core.core.shared;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiMetrics {
    private long totalTokensIn;
    private long totalTokensOut;
    private Long totalCacheWrites;
    private Long totalCacheReads;
    private double totalCost;
}
