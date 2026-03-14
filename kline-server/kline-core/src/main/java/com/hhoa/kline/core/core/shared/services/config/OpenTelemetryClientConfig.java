package com.hhoa.kline.core.core.shared.services.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpenTelemetryClientConfig {
    private boolean enabled;

    private String metricsExporter;

    private String logsExporter;

    private String otlpProtocol;

    private String otlpEndpoint;

    private String otlpMetricsProtocol;

    private String otlpMetricsEndpoint;

    private String otlpLogsProtocol;

    private String otlpLogsEndpoint;

    private Integer metricExportInterval;

    private Boolean otlpInsecure;

    private Integer logBatchSize;

    private Integer logBatchTimeout;

    private Integer logMaxQueueSize;
}
