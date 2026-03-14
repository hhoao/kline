package com.hhoa.kline.core.core.shared.api;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ModelInfo {
    private Integer maxTokens;
    private Integer contextWindow;
    private Boolean supportsImages;

    /** 此值目前是硬编码的 */
    private boolean supportsPromptCache;

    private Double inputPrice; // Keep for non-tiered input models
    private Double outputPrice; // Keep for non-tiered output models
    private ThinkingConfig thinkingConfig;

    private Boolean supportsGlobalEndpoint;

    private Double cacheWritesPrice;
    private Double cacheReadsPrice;
    private String description;
    private List<PriceTier> tiers;

    @Data
    @lombok.Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ThinkingConfig {
        private Integer maxBudget;

        private Double outputPrice;

        @lombok.Builder.Default private List<PriceTier> outputPriceTiers = new ArrayList<>();
    }

    @Data
    @lombok.Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PriceTier {
        private Integer contextWindow;
        private Double inputPrice;
        private Double outputPrice;
        private Double cacheWritesPrice;
        private Double cacheReadsPrice;
    }
}
