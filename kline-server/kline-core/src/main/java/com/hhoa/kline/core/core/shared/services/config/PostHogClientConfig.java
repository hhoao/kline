package com.hhoa.kline.core.core.shared.services.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostHogClientConfig {
    private String apiKey;

    private String errorTrackingApiKey;

    private String host;
    private String uiHost;
}
