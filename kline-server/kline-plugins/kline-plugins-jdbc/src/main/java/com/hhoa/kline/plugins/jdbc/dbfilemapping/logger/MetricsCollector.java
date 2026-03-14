package com.hhoa.kline.plugins.jdbc.dbfilemapping.logger;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 指标收集器 Collects and exposes synchronization metrics for monitoring
 *
 * <p>Requirements: 7.3, 7.5
 */
public class MetricsCollector {

    /** 每个表的指标 */
    private final Map<String, TableMetrics> tableMetricsMap;

    /** 全局指标 */
    private final GlobalMetrics globalMetrics;

    public MetricsCollector() {
        this.tableMetricsMap = new ConcurrentHashMap<>();
        this.globalMetrics = new GlobalMetrics();
    }

    /**
     * 记录成功的同步操作
     *
     * @param tableName 表名
     * @param operationType 操作类型
     * @param durationMs 操作耗时（毫秒）
     */
    public void recordSuccess(String tableName, String operationType, long durationMs) {
        TableMetrics metrics = getOrCreateTableMetrics(tableName);
        metrics.incrementSuccessCount();
        metrics.recordLatency(durationMs);

        globalMetrics.incrementSuccessCount();
        globalMetrics.recordLatency(durationMs);
    }

    /**
     * 记录失败的同步操作
     *
     * @param tableName 表名
     * @param operationType 操作类型
     */
    public void recordFailure(String tableName, String operationType) {
        TableMetrics metrics = getOrCreateTableMetrics(tableName);
        metrics.incrementFailureCount();

        globalMetrics.incrementFailureCount();
    }

    /** 记录文件到数据库同步 */
    public void recordFileToDatabaseSync(String tableName, boolean success, long durationMs) {
        if (success) {
            recordSuccess(tableName, "FILE_TO_DB", durationMs);
        } else {
            recordFailure(tableName, "FILE_TO_DB");
        }
    }

    /** 记录数据库到文件同步 */
    public void recordDatabaseToFileSync(String tableName, boolean success, long durationMs) {
        if (success) {
            recordSuccess(tableName, "DB_TO_FILE", durationMs);
        } else {
            recordFailure(tableName, "DB_TO_FILE");
        }
    }

    /** 记录初始化同步 */
    public void recordInitializationSync(String tableName, long recordCount, long durationMs) {
        TableMetrics metrics = getOrCreateTableMetrics(tableName);
        metrics.setLastInitializationTime(LocalDateTime.now());
        metrics.setLastInitializationRecordCount(recordCount);

        recordSuccess(tableName, "INIT_SYNC", durationMs);
    }

    /** 记录冲突 */
    public void recordConflict(String tableName) {
        TableMetrics metrics = getOrCreateTableMetrics(tableName);
        metrics.incrementConflictCount();

        globalMetrics.incrementConflictCount();
    }

    /** 获取表的指标 */
    public TableMetrics getTableMetrics(String tableName) {
        return tableMetricsMap.get(tableName);
    }

    /** 获取全局指标 */
    public GlobalMetrics getGlobalMetrics() {
        return globalMetrics;
    }

    /** 获取所有表的指标 */
    public Map<String, TableMetrics> getAllTableMetrics() {
        return new ConcurrentHashMap<>(tableMetricsMap);
    }

    /** 重置表的指标 */
    public void resetTableMetrics(String tableName) {
        tableMetricsMap.remove(tableName);
    }

    /** 重置所有指标 */
    public void resetAllMetrics() {
        tableMetricsMap.clear();
        globalMetrics.reset();
    }

    /** 获取或创建表指标 */
    private TableMetrics getOrCreateTableMetrics(String tableName) {
        return tableMetricsMap.computeIfAbsent(tableName, k -> new TableMetrics(tableName));
    }

    /** 表级别指标 */
    public static class TableMetrics {
        private final String tableName;
        private final AtomicLong successCount;
        private final AtomicLong failureCount;
        private final AtomicLong conflictCount;
        private final LatencyTracker latencyTracker;
        private volatile LocalDateTime lastInitializationTime;
        private volatile long lastInitializationRecordCount;

        public TableMetrics(String tableName) {
            this.tableName = tableName;
            this.successCount = new AtomicLong(0);
            this.failureCount = new AtomicLong(0);
            this.conflictCount = new AtomicLong(0);
            this.latencyTracker = new LatencyTracker();
        }

        public void incrementSuccessCount() {
            successCount.incrementAndGet();
        }

        public void incrementFailureCount() {
            failureCount.incrementAndGet();
        }

        public void incrementConflictCount() {
            conflictCount.incrementAndGet();
        }

        public void recordLatency(long latencyMs) {
            latencyTracker.record(latencyMs);
        }

        public String getTableName() {
            return tableName;
        }

        public long getSuccessCount() {
            return successCount.get();
        }

        public long getFailureCount() {
            return failureCount.get();
        }

        public long getConflictCount() {
            return conflictCount.get();
        }

        public long getTotalOperations() {
            return successCount.get() + failureCount.get();
        }

        public double getSuccessRate() {
            long total = getTotalOperations();
            if (total == 0) {
                return 0.0;
            }
            return (double) successCount.get() / total * 100.0;
        }

        public double getFailureRate() {
            long total = getTotalOperations();
            if (total == 0) {
                return 0.0;
            }
            return (double) failureCount.get() / total * 100.0;
        }

        public double getAverageLatencyMs() {
            return latencyTracker.getAverage();
        }

        public long getMinLatencyMs() {
            return latencyTracker.getMin();
        }

        public long getMaxLatencyMs() {
            return latencyTracker.getMax();
        }

        public LocalDateTime getLastInitializationTime() {
            return lastInitializationTime;
        }

        public void setLastInitializationTime(LocalDateTime lastInitializationTime) {
            this.lastInitializationTime = lastInitializationTime;
        }

        public long getLastInitializationRecordCount() {
            return lastInitializationRecordCount;
        }

        public void setLastInitializationRecordCount(long lastInitializationRecordCount) {
            this.lastInitializationRecordCount = lastInitializationRecordCount;
        }

        @Override
        public String toString() {
            return String.format(
                    "TableMetrics{table='%s', success=%d, failure=%d, conflict=%d, "
                            + "successRate=%.2f%%, avgLatency=%.2fms, minLatency=%dms, maxLatency=%dms}",
                    tableName,
                    successCount.get(),
                    failureCount.get(),
                    conflictCount.get(),
                    getSuccessRate(),
                    getAverageLatencyMs(),
                    getMinLatencyMs(),
                    getMaxLatencyMs());
        }
    }

    /** 全局指标 */
    public static class GlobalMetrics {
        private final AtomicLong successCount;
        private final AtomicLong failureCount;
        private final AtomicLong conflictCount;
        private final LatencyTracker latencyTracker;
        private final LocalDateTime startTime;

        public GlobalMetrics() {
            this.successCount = new AtomicLong(0);
            this.failureCount = new AtomicLong(0);
            this.conflictCount = new AtomicLong(0);
            this.latencyTracker = new LatencyTracker();
            this.startTime = LocalDateTime.now();
        }

        public void incrementSuccessCount() {
            successCount.incrementAndGet();
        }

        public void incrementFailureCount() {
            failureCount.incrementAndGet();
        }

        public void incrementConflictCount() {
            conflictCount.incrementAndGet();
        }

        public void recordLatency(long latencyMs) {
            latencyTracker.record(latencyMs);
        }

        public void reset() {
            successCount.set(0);
            failureCount.set(0);
            conflictCount.set(0);
            latencyTracker.reset();
        }

        public long getSuccessCount() {
            return successCount.get();
        }

        public long getFailureCount() {
            return failureCount.get();
        }

        public long getConflictCount() {
            return conflictCount.get();
        }

        public long getTotalOperations() {
            return successCount.get() + failureCount.get();
        }

        public double getSuccessRate() {
            long total = getTotalOperations();
            if (total == 0) {
                return 0.0;
            }
            return (double) successCount.get() / total * 100.0;
        }

        public double getFailureRate() {
            long total = getTotalOperations();
            if (total == 0) {
                return 0.0;
            }
            return (double) failureCount.get() / total * 100.0;
        }

        public double getAverageLatencyMs() {
            return latencyTracker.getAverage();
        }

        public long getMinLatencyMs() {
            return latencyTracker.getMin();
        }

        public long getMaxLatencyMs() {
            return latencyTracker.getMax();
        }

        public LocalDateTime getStartTime() {
            return startTime;
        }

        public Duration getUptime() {
            return Duration.between(startTime, LocalDateTime.now());
        }

        @Override
        public String toString() {
            return String.format(
                    "GlobalMetrics{success=%d, failure=%d, conflict=%d, "
                            + "successRate=%.2f%%, avgLatency=%.2fms, uptime=%s}",
                    successCount.get(),
                    failureCount.get(),
                    conflictCount.get(),
                    getSuccessRate(),
                    getAverageLatencyMs(),
                    getUptime());
        }
    }

    /** 延迟跟踪器 Tracks latency statistics (min, max, average) */
    private static class LatencyTracker {
        private final AtomicLong totalLatency;
        private final AtomicLong count;
        private volatile long minLatency;
        private volatile long maxLatency;

        public LatencyTracker() {
            this.totalLatency = new AtomicLong(0);
            this.count = new AtomicLong(0);
            this.minLatency = Long.MAX_VALUE;
            this.maxLatency = Long.MIN_VALUE;
        }

        public synchronized void record(long latencyMs) {
            totalLatency.addAndGet(latencyMs);
            count.incrementAndGet();

            if (latencyMs < minLatency) {
                minLatency = latencyMs;
            }
            if (latencyMs > maxLatency) {
                maxLatency = latencyMs;
            }
        }

        public double getAverage() {
            long cnt = count.get();
            if (cnt == 0) {
                return 0.0;
            }
            return (double) totalLatency.get() / cnt;
        }

        public long getMin() {
            return minLatency == Long.MAX_VALUE ? 0 : minLatency;
        }

        public long getMax() {
            return maxLatency == Long.MIN_VALUE ? 0 : maxLatency;
        }

        public void reset() {
            totalLatency.set(0);
            count.set(0);
            minLatency = Long.MAX_VALUE;
            maxLatency = Long.MIN_VALUE;
        }
    }
}
