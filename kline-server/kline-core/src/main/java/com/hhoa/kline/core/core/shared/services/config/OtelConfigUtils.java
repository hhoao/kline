package com.hhoa.kline.core.core.shared.services.config;

/**
 * 配置来源： - 生产构建：通过 .github/workflows/publish.yml 在构建时注入的环境变量 - 开发环境：从 .env 文件加载的环境变量（由 VSCode 加载）
 *
 * <p>支持的环境变量： - OTEL_TELEMETRY_ENABLED: "1" 启用 OpenTelemetry（默认：关闭） - OTEL_METRICS_EXPORTER:
 * 逗号分隔列表："console", "otlp", "prometheus" - OTEL_LOGS_EXPORTER: 逗号分隔列表："console", "otlp" -
 * OTEL_EXPORTER_OTLP_PROTOCOL: "grpc", "http/json" 或 "http/protobuf" - OTEL_EXPORTER_OTLP_ENDPOINT:
 * OTLP 收集器端点（如果不使用特定端点） - OTEL_EXPORTER_OTLP_METRICS_PROTOCOL: 指标特定协议覆盖 -
 * OTEL_EXPORTER_OTLP_METRICS_ENDPOINT: 指标特定端点覆盖 - OTEL_EXPORTER_OTLP_LOGS_PROTOCOL: 日志特定协议覆盖 -
 * OTEL_EXPORTER_OTLP_LOGS_ENDPOINT: 日志特定端点覆盖 - OTEL_METRIC_EXPORT_INTERVAL: 指标导出之间的毫秒数（默认：60000） -
 * OTEL_EXPORTER_OTLP_INSECURE: "true" 禁用 gRPC 的 TLS（用于本地开发） - OTEL_LOG_BATCH_SIZE:
 * 日志记录的最大批处理大小（默认：512） - OTEL_LOG_BATCH_TIMEOUT: 导出日志前等待的最大时间（毫秒）（默认：5000） -
 * OTEL_LOG_MAX_QUEUE_SIZE: 日志记录的最大队列大小（默认：2048）
 *
 * <p>配置在运行时不会更改 - 需要重新加载 VSCode 以获取新值。
 */
public class OtelConfigUtils {
    /** 缓存的 OpenTelemetry 配置 延迟初始化以避免与环境变量加载的竞态条件 */
    private static OpenTelemetryClientConfig otelConfig = null;

    private static boolean isTestEnv() {
        String e2eTest = System.getenv("E2E_TEST");
        String isTest = System.getenv("IS_TEST");
        return "true".equals(e2eTest) || "true".equals(isTest);
    }

    public static OpenTelemetryClientConfig getOtelConfig() {
        if (otelConfig == null) {
            OpenTelemetryClientConfig.OpenTelemetryClientConfigBuilder builder =
                    OpenTelemetryClientConfig.builder();

            String enabled = System.getenv("OTEL_TELEMETRY_ENABLED");
            builder.enabled("1".equals(enabled));

            builder.metricsExporter(System.getenv("OTEL_METRICS_EXPORTER"));
            builder.logsExporter(System.getenv("OTEL_LOGS_EXPORTER"));
            builder.otlpProtocol(System.getenv("OTEL_EXPORTER_OTLP_PROTOCOL"));
            builder.otlpEndpoint(System.getenv("OTEL_EXPORTER_OTLP_ENDPOINT"));
            builder.otlpMetricsProtocol(System.getenv("OTEL_EXPORTER_OTLP_METRICS_PROTOCOL"));
            builder.otlpMetricsEndpoint(System.getenv("OTEL_EXPORTER_OTLP_METRICS_ENDPOINT"));
            builder.otlpLogsProtocol(System.getenv("OTEL_EXPORTER_OTLP_LOGS_PROTOCOL"));
            builder.otlpLogsEndpoint(System.getenv("OTEL_EXPORTER_OTLP_LOGS_ENDPOINT"));

            String metricExportInterval = System.getenv("OTEL_METRIC_EXPORT_INTERVAL");
            if (metricExportInterval != null) {
                try {
                    builder.metricExportInterval(Integer.parseInt(metricExportInterval));
                } catch (NumberFormatException e) {
                }
            }

            String otlpInsecure = System.getenv("OTEL_EXPORTER_OTLP_INSECURE");
            builder.otlpInsecure("true".equals(otlpInsecure));

            String logBatchSize = System.getenv("OTEL_LOG_BATCH_SIZE");
            if (logBatchSize != null) {
                try {
                    int size = Integer.parseInt(logBatchSize);
                    builder.logBatchSize(Math.max(1, size));
                } catch (NumberFormatException e) {
                }
            }

            String logBatchTimeout = System.getenv("OTEL_LOG_BATCH_TIMEOUT");
            if (logBatchTimeout != null) {
                try {
                    int timeout = Integer.parseInt(logBatchTimeout);
                    builder.logBatchTimeout(Math.max(1, timeout));
                } catch (NumberFormatException e) {
                }
            }

            String logMaxQueueSize = System.getenv("OTEL_LOG_MAX_QUEUE_SIZE");
            if (logMaxQueueSize != null) {
                try {
                    int maxSize = Integer.parseInt(logMaxQueueSize);
                    builder.logMaxQueueSize(Math.max(1, maxSize));
                } catch (NumberFormatException e) {
                }
            }

            otelConfig = builder.build();
        }
        return otelConfig;
    }

    public static boolean isOpenTelemetryConfigValid(OpenTelemetryClientConfig config) {
        if (isTestEnv()) {
            return false;
        }

        if (!config.isEnabled()) {
            return false;
        }

        return (config.getMetricsExporter() != null && !config.getMetricsExporter().isEmpty())
                || (config.getLogsExporter() != null && !config.getLogsExporter().isEmpty());
    }

    public static OpenTelemetryClientConfig getValidOpenTelemetryConfig() {
        OpenTelemetryClientConfig config = getOtelConfig();
        if (isOpenTelemetryConfigValid(config)) {
            return OpenTelemetryClientConfig.builder()
                    .enabled(true)
                    .metricsExporter(config.getMetricsExporter())
                    .logsExporter(config.getLogsExporter())
                    .otlpProtocol(config.getOtlpProtocol())
                    .otlpEndpoint(config.getOtlpEndpoint())
                    .otlpMetricsProtocol(config.getOtlpMetricsProtocol())
                    .otlpMetricsEndpoint(config.getOtlpMetricsEndpoint())
                    .otlpLogsProtocol(config.getOtlpLogsProtocol())
                    .otlpLogsEndpoint(config.getOtlpLogsEndpoint())
                    .metricExportInterval(config.getMetricExportInterval())
                    .otlpInsecure(config.getOtlpInsecure())
                    .logBatchSize(config.getLogBatchSize())
                    .logBatchTimeout(config.getLogBatchTimeout())
                    .logMaxQueueSize(config.getLogMaxQueueSize())
                    .build();
        }
        return null;
    }
}
