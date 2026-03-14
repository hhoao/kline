package com.hhoa.kline.plugins.jdbc.dbfilemapping.helper;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 同步上下文管理器 Manages synchronization context to prevent circular triggers
 *
 * <p>使用双向计数器机制来避免双向同步时的循环触发
 *
 * <p>工作原理： 1. 维护两个独立的计数器 Map：fileToDbCounters 和 dbToFileCounters 2. 文件修改时：fileToDbCounters
 * +1，执行同步，完成后不减少 3. 数据库回调时：检查 fileToDbCounters，如果 > 0 则 -1 并跳过同步 4. 数据库修改时：dbToFileCounters
 * +1，执行同步，完成后不减少 5. 文件回调时：检查 dbToFileCounters，如果 > 0 则 -1 并跳过同步 6. 这样可以支持连续修改，计数器会累积，反向回调会逐步抵消
 */
public class SyncContext {

    /** 同步方向枚举 */
    public enum SyncDirection {
        FILE_TO_DB, // 文件到数据库
        DB_TO_FILE // 数据库到文件
    }

    /** 同步计数记录 */
    private static class SyncCounter {
        final AtomicInteger counter;
        volatile long lastAccessTime;

        SyncCounter() {
            this.counter = new AtomicInteger(0);
            this.lastAccessTime = System.currentTimeMillis();
        }

        int increment() {
            lastAccessTime = System.currentTimeMillis();
            return counter.incrementAndGet();
        }

        int decrement() {
            lastAccessTime = System.currentTimeMillis();
            int value = counter.decrementAndGet();
            // 确保不会变成负数
            if (value < 0) {
                counter.set(0);
                return 0;
            }
            return value;
        }

        int get() {
            lastAccessTime = System.currentTimeMillis();
            return counter.get();
        }

        boolean isExpired(long timeoutMs) {
            return (System.currentTimeMillis() - lastAccessTime) > timeoutMs;
        }
    }

    /** 文件到数据库的同步计数器 格式: "schema.table:primaryKey" -> SyncCounter */
    private static final Map<String, SyncCounter> fileToDbCounters = new ConcurrentHashMap<>();

    /** 数据库到文件的同步计数器 格式: "schema.table:primaryKey" -> SyncCounter */
    private static final Map<String, SyncCounter> dbToFileCounters = new ConcurrentHashMap<>();

    /** 清理超时时间（毫秒） 超过此时间未访问的记录会被清理 */
    private static final long CLEANUP_TIMEOUT_MS = 60000; // 60秒

    /**
     * 增加同步计数
     *
     * @param schemaName schema名称
     * @param tableName 表名
     * @param primaryKey 主键值
     * @param direction 同步方向
     */
    public static void incrementSync(
            String schemaName, String tableName, Object primaryKey, SyncDirection direction) {
        String key = makeKey(schemaName, tableName, primaryKey);
        Map<String, SyncCounter> counters = getCounterMap(direction);

        SyncCounter counter = counters.computeIfAbsent(key, k -> new SyncCounter());
        counter.increment();

        // 清理过期记录（简单实现，避免内存泄漏）
        if (counters.size() > 10000) {
            cleanupExpiredRecords(counters);
        }
    }

    /**
     * 增加同步计数（指定增加的数量） 用于 FIELD_FILES 模式，一次写入多个字段文件时只增加一次计数
     *
     * @param schemaName schema名称
     * @param tableName 表名
     * @param primaryKey 主键值
     * @param direction 同步方向
     * @param count 增加的数量
     */
    public static void incrementSyncBy(
            String schemaName,
            String tableName,
            Object primaryKey,
            SyncDirection direction,
            int count) {
        String key = makeKey(schemaName, tableName, primaryKey);
        Map<String, SyncCounter> counters = getCounterMap(direction);

        SyncCounter counter = counters.computeIfAbsent(key, k -> new SyncCounter());
        for (int i = 0; i < count; i++) {
            counter.increment();
        }

        // 清理过期记录（简单实现，避免内存泄漏）
        if (counters.size() > 10000) {
            cleanupExpiredRecords(counters);
        }
    }

    /**
     * 检查并减少反向同步计数
     *
     * @param schemaName schema名称
     * @param tableName 表名
     * @param primaryKey 主键值
     * @param direction 当前同步方向
     * @return true 如果应该跳过（反向计数器 > 0）
     */
    public static boolean checkAndDecrementOppositeSync(
            String schemaName, String tableName, Object primaryKey, SyncDirection direction) {
        String key = makeKey(schemaName, tableName, primaryKey);

        // 获取反向计数器
        SyncDirection oppositeDirection = getOppositeDirection(direction);
        Map<String, SyncCounter> oppositeCounters = getCounterMap(oppositeDirection);

        SyncCounter counter = oppositeCounters.get(key);
        if (counter == null) {
            return false;
        }

        int count = counter.get();
        if (count > 0) {
            // 反向计数器 > 0，减少计数并跳过
            counter.decrement();
            return true;
        }

        return false;
    }

    /** 获取对应方向的计数器 Map */
    private static Map<String, SyncCounter> getCounterMap(SyncDirection direction) {
        return direction == SyncDirection.FILE_TO_DB ? fileToDbCounters : dbToFileCounters;
    }

    /** 获取相反的同步方向 */
    private static SyncDirection getOppositeDirection(SyncDirection direction) {
        return direction == SyncDirection.FILE_TO_DB
                ? SyncDirection.DB_TO_FILE
                : SyncDirection.FILE_TO_DB;
    }

    /** 生成记录键 */
    private static String makeKey(String schemaName, String tableName, Object primaryKey) {
        String schema = (schemaName == null || schemaName.trim().isEmpty()) ? "public" : schemaName;
        return schema + "." + tableName + ":" + primaryKey;
    }

    /** 清理过期记录 */
    private static void cleanupExpiredRecords(Map<String, SyncCounter> counters) {
        counters.entrySet()
                .removeIf(
                        entry ->
                                entry.getValue().isExpired(CLEANUP_TIMEOUT_MS)
                                        && entry.getValue().get() == 0);
    }

    /** 清理所有同步上下文（用于测试或重置） */
    public static void clear() {
        fileToDbCounters.clear();
        dbToFileCounters.clear();
    }

    /** 获取当前记录数量 */
    public static int getRecordCount() {
        return fileToDbCounters.size() + dbToFileCounters.size();
    }

    /** 获取指定记录的计数器值（用于调试） */
    public static int getCounter(
            String schemaName, String tableName, Object primaryKey, SyncDirection direction) {
        String key = makeKey(schemaName, tableName, primaryKey);
        Map<String, SyncCounter> counters = getCounterMap(direction);
        SyncCounter counter = counters.get(key);
        return counter != null ? counter.get() : 0;
    }
}
