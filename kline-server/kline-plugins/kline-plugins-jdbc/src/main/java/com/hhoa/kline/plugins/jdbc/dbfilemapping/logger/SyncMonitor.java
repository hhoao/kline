package com.hhoa.kline.plugins.jdbc.dbfilemapping.logger;

import java.util.HashMap;
import java.util.Map;

/**
 * 同步监控接口 Provides monitoring interface to expose synchronization metrics
 *
 * <p>Requirements: 7.5
 */
public class SyncMonitor {

    private final MetricsCollector metricsCollector;

    public SyncMonitor(MetricsCollector metricsCollector) {
        this.metricsCollector = metricsCollector;
    }

    /** 获取全局指标摘要 Returns a summary of global metrics for monitoring systems */
    public Map<String, Object> getGlobalMetricsSummary() {
        MetricsCollector.GlobalMetrics global = metricsCollector.getGlobalMetrics();

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalOperations", global.getTotalOperations());
        summary.put("successCount", global.getSuccessCount());
        summary.put("failureCount", global.getFailureCount());
        summary.put("conflictCount", global.getConflictCount());
        summary.put("successRate", String.format("%.2f%%", global.getSuccessRate()));
        summary.put("failureRate", String.format("%.2f%%", global.getFailureRate()));
        summary.put("averageLatencyMs", String.format("%.2f", global.getAverageLatencyMs()));
        summary.put("minLatencyMs", global.getMinLatencyMs());
        summary.put("maxLatencyMs", global.getMaxLatencyMs());
        summary.put("uptimeSeconds", global.getUptime().getSeconds());
        summary.put("startTime", global.getStartTime().toString());

        return summary;
    }

    /** 获取指定表的指标摘要 */
    public Map<String, Object> getTableMetricsSummary(String tableName) {
        MetricsCollector.TableMetrics table = metricsCollector.getTableMetrics(tableName);

        if (table == null) {
            return null;
        }

        Map<String, Object> summary = new HashMap<>();
        summary.put("tableName", table.getTableName());
        summary.put("totalOperations", table.getTotalOperations());
        summary.put("successCount", table.getSuccessCount());
        summary.put("failureCount", table.getFailureCount());
        summary.put("conflictCount", table.getConflictCount());
        summary.put("successRate", String.format("%.2f%%", table.getSuccessRate()));
        summary.put("failureRate", String.format("%.2f%%", table.getFailureRate()));
        summary.put("averageLatencyMs", String.format("%.2f", table.getAverageLatencyMs()));
        summary.put("minLatencyMs", table.getMinLatencyMs());
        summary.put("maxLatencyMs", table.getMaxLatencyMs());

        if (table.getLastInitializationTime() != null) {
            summary.put("lastInitializationTime", table.getLastInitializationTime().toString());
            summary.put("lastInitializationRecordCount", table.getLastInitializationRecordCount());
        }

        return summary;
    }

    /** 获取所有表的指标摘要 */
    public Map<String, Map<String, Object>> getAllTableMetricsSummaries() {
        Map<String, Map<String, Object>> allSummaries = new HashMap<>();

        Map<String, MetricsCollector.TableMetrics> allMetrics =
                metricsCollector.getAllTableMetrics();
        for (Map.Entry<String, MetricsCollector.TableMetrics> entry : allMetrics.entrySet()) {
            allSummaries.put(entry.getKey(), getTableMetricsSummary(entry.getKey()));
        }

        return allSummaries;
    }

    /**
     * 获取完整的监控报告 Returns a comprehensive monitoring report including global and per-table metrics
     */
    public Map<String, Object> getFullMonitoringReport() {
        Map<String, Object> report = new HashMap<>();
        report.put("global", getGlobalMetricsSummary());
        report.put("tables", getAllTableMetricsSummaries());
        return report;
    }

    /** 获取健康状态 Returns health status based on failure rate and recent activity */
    public HealthStatus getHealthStatus() {
        MetricsCollector.GlobalMetrics global = metricsCollector.getGlobalMetrics();

        double failureRate = global.getFailureRate();
        long totalOps = global.getTotalOperations();

        // No operations yet
        if (totalOps == 0) {
            return new HealthStatus("UNKNOWN", "No operations recorded yet");
        }

        // High failure rate
        if (failureRate > 50.0) {
            return new HealthStatus(
                    "CRITICAL", String.format("High failure rate: %.2f%%", failureRate));
        }

        // Moderate failure rate
        if (failureRate > 20.0) {
            return new HealthStatus(
                    "WARNING", String.format("Elevated failure rate: %.2f%%", failureRate));
        }

        // Healthy
        return new HealthStatus(
                "HEALTHY",
                String.format(
                        "System operating normally. Success rate: %.2f%%",
                        global.getSuccessRate()));
    }

    /** 获取Prometheus格式的指标 Returns metrics in Prometheus exposition format */
    public String getPrometheusMetrics() {
        StringBuilder sb = new StringBuilder();

        MetricsCollector.GlobalMetrics global = metricsCollector.getGlobalMetrics();

        // Global metrics
        sb.append("# HELP sync_operations_total Total number of sync operations\n");
        sb.append("# TYPE sync_operations_total counter\n");
        sb.append("sync_operations_total{result=\"success\"} ")
                .append(global.getSuccessCount())
                .append("\n");
        sb.append("sync_operations_total{result=\"failure\"} ")
                .append(global.getFailureCount())
                .append("\n");

        sb.append("# HELP sync_conflicts_total Total number of conflicts detected\n");
        sb.append("# TYPE sync_conflicts_total counter\n");
        sb.append("sync_conflicts_total ").append(global.getConflictCount()).append("\n");

        sb.append("# HELP sync_latency_ms Sync operation latency in milliseconds\n");
        sb.append("# TYPE sync_latency_ms summary\n");
        sb.append("sync_latency_ms{quantile=\"min\"} ")
                .append(global.getMinLatencyMs())
                .append("\n");
        sb.append("sync_latency_ms{quantile=\"max\"} ")
                .append(global.getMaxLatencyMs())
                .append("\n");
        sb.append("sync_latency_ms{quantile=\"avg\"} ")
                .append(String.format("%.2f", global.getAverageLatencyMs()))
                .append("\n");

        sb.append("# HELP sync_uptime_seconds System uptime in seconds\n");
        sb.append("# TYPE sync_uptime_seconds gauge\n");
        sb.append("sync_uptime_seconds ").append(global.getUptime().getSeconds()).append("\n");

        // Per-table metrics
        Map<String, MetricsCollector.TableMetrics> allMetrics =
                metricsCollector.getAllTableMetrics();
        for (MetricsCollector.TableMetrics table : allMetrics.values()) {
            String tableName = table.getTableName();

            sb.append("sync_table_operations_total{table=\"")
                    .append(tableName)
                    .append("\",result=\"success\"} ")
                    .append(table.getSuccessCount())
                    .append("\n");
            sb.append("sync_table_operations_total{table=\"")
                    .append(tableName)
                    .append("\",result=\"failure\"} ")
                    .append(table.getFailureCount())
                    .append("\n");
            sb.append("sync_table_conflicts_total{table=\"")
                    .append(tableName)
                    .append("\"} ")
                    .append(table.getConflictCount())
                    .append("\n");
        }

        return sb.toString();
    }

    /** 健康状态类 */
    @lombok.Value
    public static class HealthStatus {
        String status;
        String message;

        public boolean isHealthy() {
            return "HEALTHY".equals(status);
        }

        @Override
        public String toString() {
            return String.format("HealthStatus{status='%s', message='%s'}", status, message);
        }
    }
}
