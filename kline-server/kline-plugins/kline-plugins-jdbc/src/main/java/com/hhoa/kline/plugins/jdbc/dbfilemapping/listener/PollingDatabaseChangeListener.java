package com.hhoa.kline.plugins.jdbc.dbfilemapping.listener;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 基于轮询的数据库变更监听器 Polling-based database change listener as an alternative to trigger-based listening
 *
 * <p>通过定期查询数据库的 update_time 字段来检测变更 Detects changes by periodically querying the update_time column
 */
public class PollingDatabaseChangeListener implements DatabaseChangeListener {

    private static final Logger logger =
            LoggerFactory.getLogger(PollingDatabaseChangeListener.class);

    /** 轮询间隔（毫秒） */
    private static final long POLL_INTERVAL_MILLIS = 5000;

    /** 关闭超时时间（秒） */
    private static final int SHUTDOWN_TIMEOUT_SECONDS = 10;

    private final JdbcTemplate jdbcTemplate;

    /** 运行状态标志 */
    private final AtomicBoolean running = new AtomicBoolean(false);

    /** 轮询线程池 */
    private ScheduledExecutorService pollingExecutor;

    /** 表名到回调的映射 */
    private final Map<String, ChangeCallback> tableCallbackMap = new ConcurrentHashMap<>();

    /** 表名到最后检查时间的映射 */
    private final Map<String, LocalDateTime> lastCheckTimeMap = new ConcurrentHashMap<>();

    /** 表名到主键列名的映射 */
    private final Map<String, String> tablePrimaryKeyMap = new ConcurrentHashMap<>();

    public PollingDatabaseChangeListener(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void listenToTable(String tableName, ChangeCallback callback)
            throws DatabaseListenerException {
        if (tableName == null || tableName.trim().isEmpty()) {
            throw new DatabaseListenerException("Table name cannot be null or empty");
        }

        if (callback == null) {
            throw new DatabaseListenerException("Callback cannot be null");
        }

        if (!running.get()) {
            throw new DatabaseListenerException(
                    "PollingDatabaseChangeListener is not running. Call start() first.");
        }

        // 保存回调
        tableCallbackMap.put(tableName, callback);

        // 初始化最后检查时间为当前时间
        lastCheckTimeMap.put(tableName, LocalDateTime.now());

        // 尝试检测主键列名
        String primaryKeyColumn = detectPrimaryKeyColumn(tableName);
        if (primaryKeyColumn != null) {
            tablePrimaryKeyMap.put(tableName, primaryKeyColumn);
            logger.info(
                    "Detected primary key column for table {}: {}", tableName, primaryKeyColumn);
        } else {
            logger.warn("Could not detect primary key column for table: {}", tableName);
        }

        logger.info("Registered polling listener for table: {}", tableName);
    }

    @Override
    public void stopListening(String tableName) {
        if (tableName == null || tableName.trim().isEmpty()) {
            return;
        }

        tableCallbackMap.remove(tableName);
        lastCheckTimeMap.remove(tableName);
        tablePrimaryKeyMap.remove(tableName);

        logger.info("Unregistered polling listener for table: {}", tableName);
    }

    @Override
    public void start() throws DatabaseListenerException {
        if (running.get()) {
            logger.warn("PollingDatabaseChangeListener is already running");
            return;
        }

        logger.info("Starting PollingDatabaseChangeListener...");

        running.set(true);

        // 创建轮询线程池
        pollingExecutor =
                Executors.newScheduledThreadPool(
                        1,
                        r -> {
                            Thread thread = new Thread(r, "PollingDatabaseChangeListener-Thread");
                            thread.setDaemon(true);
                            return thread;
                        });

        // 启动轮询任务
        pollingExecutor.scheduleWithFixedDelay(
                this::pollTables,
                POLL_INTERVAL_MILLIS, // 初始延迟
                POLL_INTERVAL_MILLIS, // 轮询间隔
                TimeUnit.MILLISECONDS);

        logger.info(
                "PollingDatabaseChangeListener started (polling interval: {}ms)",
                POLL_INTERVAL_MILLIS);
    }

    @Override
    public void stop() {
        if (!running.get()) {
            logger.warn("PollingDatabaseChangeListener is not running");
            return;
        }

        logger.info("Stopping PollingDatabaseChangeListener...");
        running.set(false);

        // 关闭线程池
        if (pollingExecutor != null) {
            pollingExecutor.shutdown();
            try {
                if (!pollingExecutor.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                    pollingExecutor.shutdownNow();
                    logger.warn("Polling thread did not terminate gracefully");
                }
            } catch (InterruptedException e) {
                pollingExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        // 清理映射
        tableCallbackMap.clear();
        lastCheckTimeMap.clear();
        tablePrimaryKeyMap.clear();

        logger.info("PollingDatabaseChangeListener stopped");
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    /** 轮询所有注册的表 */
    private void pollTables() {
        for (Map.Entry<String, ChangeCallback> entry : tableCallbackMap.entrySet()) {
            String tableName = entry.getKey();
            ChangeCallback callback = entry.getValue();

            try {
                checkTableForChanges(tableName, callback);
            } catch (Exception e) {
                logger.error("Error polling table: {}", tableName, e);
            }
        }
    }

    /** 检查表的变更 */
    private void checkTableForChanges(String tableName, ChangeCallback callback) {
        LocalDateTime lastCheckTime = lastCheckTimeMap.get(tableName);
        if (lastCheckTime == null) {
            lastCheckTime = LocalDateTime.now().minusMinutes(1);
        }

        String primaryKeyColumn = tablePrimaryKeyMap.get(tableName);
        if (primaryKeyColumn == null) {
            logger.debug("No primary key column found for table: {}, skipping", tableName);
            return;
        }

        try {
            // 查询自上次检查以来更新的记录
            String sql =
                    String.format(
                            "SELECT %s, update_time FROM %s WHERE update_time > ? ORDER BY update_time",
                            primaryKeyColumn, tableName);

            List<Map<String, Object>> changedRecords =
                    jdbcTemplate.queryForList(sql, lastCheckTime);

            if (!changedRecords.isEmpty()) {
                logger.debug(
                        "Found {} changed records in table: {}", changedRecords.size(), tableName);

                for (Map<String, Object> record : changedRecords) {
                    Object primaryKey = record.get(primaryKeyColumn);

                    if (primaryKey != null) {
                        // 触发更新回调
                        callback.onUpdate(tableName, primaryKey);
                    }
                }

                // 更新最后检查时间为最新记录的时间
                Object lastUpdateTime =
                        changedRecords.get(changedRecords.size() - 1).get("update_time");
                if (lastUpdateTime instanceof LocalDateTime) {
                    lastCheckTimeMap.put(tableName, (LocalDateTime) lastUpdateTime);
                }
            }

            // 更新最后检查时间（即使没有变更）
            lastCheckTimeMap.put(tableName, LocalDateTime.now());

        } catch (Exception e) {
            logger.error("Error checking table for changes: {}", tableName, e);
        }
    }

    /** 检测表的主键列名 */
    private String detectPrimaryKeyColumn(String tableName) {
        try {
            // 解析 schema.table
            String[] parts = tableName.split("\\.");
            String schemaName = parts.length > 1 ? parts[0] : "public";
            String tableNameOnly = parts.length > 1 ? parts[1] : parts[0];

            // 查询主键列
            String sql =
                    "SELECT a.attname "
                            + "FROM pg_index i "
                            + "JOIN pg_attribute a ON a.attrelid = i.indrelid AND a.attnum = ANY(i.indkey) "
                            + "WHERE i.indrelid = ?::regclass AND i.indisprimary";

            String fullTableName = schemaName + "." + tableNameOnly;
            List<String> primaryKeys = jdbcTemplate.queryForList(sql, String.class, fullTableName);

            if (!primaryKeys.isEmpty()) {
                return primaryKeys.get(0); // 返回第一个主键列
            }

        } catch (Exception e) {
            logger.warn("Failed to detect primary key for table: {}", tableName, e);
        }

        return null;
    }
}
