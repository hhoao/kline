package com.hhoa.kline.plugins.jdbc.dbfilemapping.listener;

import com.hhoa.kline.plugins.jdbc.service.JdbcService;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.sql.DataSource;
import org.postgresql.PGConnection;
import org.postgresql.PGNotification;
import org.postgresql.jdbc.PgConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 默认数据库变更监听器实现 Default implementation of DatabaseChangeListener using PostgreSQL LISTEN/NOTIFY
 *
 * <p>Requirements: 10.4
 */
public class DefaultDatabaseChangeListener implements DatabaseChangeListener {

    private static final Logger logger =
            LoggerFactory.getLogger(DefaultDatabaseChangeListener.class);

    /** JDBC服务 */
    private final JdbcService jdbcService;

    /** 数据源（用于获取 PostgreSQL 连接） */
    private final DataSource dataSource;

    /** 监听线程 */
    private ExecutorService listenerThread;

    /** 运行状态标志 */
    private final AtomicBoolean running = new AtomicBoolean(false);

    /** 表名到回调的映射 */
    private final Map<String, ChangeCallback> tableCallbackMap = new ConcurrentHashMap<>();

    /** Channel到表名的映射 */
    private final Map<String, String> channelToTableMap = new ConcurrentHashMap<>();

    /** PostgreSQL连接 */
    private volatile PgConnection pgConnection;

    /** 关闭超时时间（秒） Timeout for graceful shutdown */
    private static final int SHUTDOWN_TIMEOUT_SECONDS = 10;

    /** 通知检查间隔（毫秒） */
    private static final int NOTIFICATION_CHECK_INTERVAL_MS = 500;

    public DefaultDatabaseChangeListener(JdbcService jdbcService) {
        if (jdbcService == null) {
            throw new IllegalArgumentException("JdbcService cannot be null");
        }
        this.jdbcService = jdbcService;
        this.dataSource = jdbcService.getDataSource();
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
                    "DatabaseChangeListener is not running. Call start() first.");
        }

        try {
            String channel = buildChannelName(tableName);
            String triggerName = buildTriggerName(tableName);
            ensureTriggerExists(tableName, channel, triggerName);

            jdbcService.executeListen(channel);
            logger.info("Started listening to channel: {}", channel);

            // 保存回调
            tableCallbackMap.put(tableName, callback);

            // 保存 channel 到表名的映射
            channelToTableMap.put(channel, tableName);

            logger.info("Registered listener for table: {}", tableName);

        } catch (Exception e) {
            throw new DatabaseListenerException("Failed to listen to table: " + tableName, e);
        }
    }

    private String buildChannelName(String tableName) {
        return "table_change_" + tableName.replace(".", "_");
    }

    private String buildTriggerName(String tableName) {
        return "trigger_" + tableName.replace(".", "_");
    }

    private void ensureTriggerExists(String tableName, String channel, String triggerName) {
        String[] parts = tableName.split("\\.");
        String schemaName = parts.length > 1 ? parts[0] : "public";
        String tableNameOnly = parts.length > 1 ? parts[1] : parts[0];

        if (jdbcService.triggerExists(schemaName, tableNameOnly, triggerName)) {
            logger.debug("Trigger already exists for table: {}", tableName);
            return;
        }

        logger.info("Creating trigger for table: {}", tableName);
        jdbcService.createNotificationTrigger(schemaName, tableNameOnly, channel, triggerName);
    }

    @Override
    public void stopListening(String tableName) {
        if (tableName == null || tableName.trim().isEmpty()) {
            return;
        }

        try {
            String channel = buildChannelName(tableName);

            if (pgConnection != null && !pgConnection.isClosed()) {
                try {
                    jdbcService.executeUnlisten(channel);
                    logger.info("Stopped listening to channel: {}", channel);
                } catch (Exception e) {
                    logger.warn("Error executing UNLISTEN for channel: {}", channel, e);
                }
            }

            // 移除回调
            tableCallbackMap.remove(tableName);

            // 移除 channel 映射
            channelToTableMap.remove(channel);

            logger.info("Unregistered listener for table: {}", tableName);

        } catch (SQLException e) {
            logger.error("Error stopping listener for table: {}", tableName, e);
        }
    }

    @Override
    public void start() throws DatabaseListenerException {
        if (running.get()) {
            logger.warn("DatabaseChangeListener is already running");
            return;
        }

        logger.info("Starting DatabaseChangeListener...");

        try {
            // 获取PostgreSQL连接
            pgConnection = dataSource.getConnection().unwrap(PgConnection.class);

            // 设置自动提交
            pgConnection.setAutoCommit(true);

            running.set(true);

            // 启动监听线程
            listenerThread =
                    Executors.newSingleThreadExecutor(
                            r -> {
                                Thread thread = new Thread(r, "DatabaseChangeListener-Thread");
                                thread.setDaemon(true);
                                return thread;
                            });

            listenerThread.submit(this::listenLoop);

            logger.info("DatabaseChangeListener started successfully");

        } catch (SQLException e) {
            throw new DatabaseListenerException("Failed to start DatabaseChangeListener", e);
        }
    }

    @Override
    public void stop() {
        if (!running.get()) {
            logger.warn("DatabaseChangeListener is not running");
            return;
        }

        logger.info("Stopping DatabaseChangeListener - initiating graceful shutdown...");
        running.set(false);

        try {
            // Step 1: 停止监听所有表
            // Stop listening to all tables
            logger.debug("Unregistering {} table listeners...", tableCallbackMap.size());
            for (String tableName : tableCallbackMap.keySet()) {
                try {
                    stopListening(tableName);
                } catch (Exception e) {
                    logger.warn("Error stopping listener for table: {}", tableName, e);
                }
            }
            tableCallbackMap.clear();

            // Step 2: 关闭数据库连接
            // Close database connection
            if (pgConnection != null) {
                try {
                    logger.debug("Closing PostgreSQL connection...");

                    // 取消所有LISTEN
                    if (!pgConnection.isClosed()) {
                        try {
                            jdbcService.executeUnlistenAll();
                            logger.debug("Executed UNLISTEN * to cancel all listeners");
                        } catch (Exception e) {
                            logger.warn("Error executing UNLISTEN *", e);
                        }
                    }

                    // 关闭连接
                    pgConnection.close();
                    logger.debug("PostgreSQL connection closed");

                } catch (SQLException e) {
                    logger.error("Error closing PostgreSQL connection", e);
                } finally {
                    pgConnection = null;
                }
            }

            // Step 3: 优雅关闭监听线程（等待进行中的任务完成）
            // Gracefully shutdown listener thread with timeout
            if (listenerThread != null) {
                logger.debug("Shutting down listener thread...");
                listenerThread.shutdown();

                try {
                    // 等待线程完成，最多等待配置的超时时间
                    // Wait for thread to complete, up to configured timeout
                    boolean terminated =
                            listenerThread.awaitTermination(
                                    SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS);

                    if (!terminated) {
                        // 超时后强制关闭
                        // Force shutdown after timeout
                        logger.warn(
                                "Listener thread did not terminate within {} seconds, forcing shutdown",
                                SHUTDOWN_TIMEOUT_SECONDS);
                        listenerThread.shutdownNow();

                        // 再等待一小段时间确认强制关闭
                        // Wait a bit more to confirm forced shutdown
                        if (!listenerThread.awaitTermination(2, TimeUnit.SECONDS)) {
                            logger.error(
                                    "Listener thread did not terminate even after forced shutdown");
                        } else {
                            logger.info("Listener thread terminated after forced shutdown");
                        }
                    } else {
                        logger.debug("Listener thread terminated gracefully");
                    }

                } catch (InterruptedException e) {
                    // 如果等待被中断，强制关闭
                    // If waiting is interrupted, force shutdown
                    logger.warn(
                            "Interrupted while waiting for listener thread termination, forcing shutdown");
                    listenerThread.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }

            logger.info("DatabaseChangeListener stopped successfully - all resources cleaned up");

        } catch (Exception e) {
            logger.error(
                    "Error during DatabaseChangeListener shutdown, some resources may not be cleaned up properly",
                    e);

            // 尽力清理剩余资源
            // Best effort cleanup of remaining resources
            try {
                if (pgConnection != null && !pgConnection.isClosed()) {
                    pgConnection.close();
                }
                if (listenerThread != null && !listenerThread.isShutdown()) {
                    listenerThread.shutdownNow();
                }
                tableCallbackMap.clear();
                channelToTableMap.clear();
            } catch (Exception cleanupEx) {
                logger.error("Error during cleanup after shutdown failure", cleanupEx);
            }
        }
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    /** 监听循环 Main listen loop that processes PostgreSQL notifications */
    private void listenLoop() {
        logger.info("DatabaseChangeListener loop started");

        while (running.get()) {
            try {
                // 检查连接是否有效
                if (pgConnection == null || pgConnection.isClosed()) {
                    logger.error("PostgreSQL connection is closed, exiting listen loop");
                    break;
                }

                // 获取PGConnection以访问通知
                PGConnection pgConn = pgConnection.unwrap(PGConnection.class);

                // 检查是否有通知
                PGNotification[] notifications = pgConn.getNotifications();

                if (notifications != null) {
                    for (PGNotification notification : notifications) {
                        processNotification(notification);
                    }
                }

                // 短暂休眠以避免CPU占用过高
                Thread.sleep(NOTIFICATION_CHECK_INTERVAL_MS);

            } catch (InterruptedException e) {
                logger.info("DatabaseChangeListener loop interrupted");
                Thread.currentThread().interrupt();
                break;
            } catch (SQLException e) {
                logger.error("SQL error in listen loop", e);
                handleListenLoopException(e);
            } catch (Exception e) {
                logger.error("Error in listen loop", e);
                handleListenLoopException(e);
            }
        }

        logger.info("DatabaseChangeListener loop ended");
    }

    /** 处理PostgreSQL通知 */
    private void processNotification(PGNotification notification) {
        try {
            String channel = notification.getName();
            String payload = notification.getParameter();

            logger.debug("Received notification: channel={}, payload={}", channel, payload);

            // 解析JSON payload
            // 格式: {"operation":"INSERT","table":"users","primary_key":"123"}
            String operation = extractJsonValue(payload, "operation");
            String tableNameOnly = extractJsonValue(payload, "table");
            String primaryKey = extractJsonValue(payload, "primary_key");

            if (operation == null || tableNameOnly == null || primaryKey == null) {
                logger.warn("Invalid notification payload: {}", payload);
                return;
            }

            // 从 channel 提取 schema 名称，然后组合完整表名
            // channel 格式: table_change_schema_table
            String tableName = extractFullTableName(channel, tableNameOnly);

            ChangeCallback callback = tableCallbackMap.get(tableName);
            if (callback == null) {
                logger.warn("No callback registered for table: {}", tableName);
                return;
            }

            // 调用相应的回调
            switch (operation.toUpperCase()) {
                case "INSERT":
                    callback.onInsert(tableName, primaryKey);
                    break;
                case "UPDATE":
                    callback.onUpdate(tableName, primaryKey);
                    break;
                case "DELETE":
                    callback.onDelete(tableName, primaryKey);
                    break;
                default:
                    logger.warn("Unknown operation: {}", operation);
            }

        } catch (Exception e) {
            logger.error("Error processing notification", e);
        }
    }

    /** 从 channel 和表名提取完整的表名（包含 schema） channel 格式: table_change_schema_table */
    private String extractFullTableName(String channel, String tableNameOnly) {
        // 先尝试从映射中获取
        String mappedTableName = channelToTableMap.get(channel);
        if (mappedTableName != null) {
            return mappedTableName;
        }

        // 如果映射中没有，尝试从 channel 中提取 schema
        if (channel.startsWith("table_change_")) {
            String suffix = channel.substring("table_change_".length());

            // 如果 suffix 以表名结尾，提取 schema
            if (suffix.endsWith("_" + tableNameOnly)) {
                String schema = suffix.substring(0, suffix.length() - tableNameOnly.length() - 1);
                return schema + "." + tableNameOnly;
            }
        }

        // 默认使用 public schema
        return "public." + tableNameOnly;
    }

    /** 从JSON字符串中提取值（简单实现，避免引入JSON库依赖） */
    private String extractJsonValue(String json, String key) {
        try {
            String searchKey = "\"" + key + "\"";
            int keyIndex = json.indexOf(searchKey);
            if (keyIndex == -1) {
                return null;
            }

            // 找到冒号
            int colonIndex = json.indexOf(":", keyIndex);
            if (colonIndex == -1) {
                return null;
            }

            // 跳过空格和引号
            int valueStart = colonIndex + 1;
            while (valueStart < json.length()
                    && (json.charAt(valueStart) == ' ' || json.charAt(valueStart) == '"')) {
                valueStart++;
            }

            // 找到值的结束位置
            int valueEnd = valueStart;
            while (valueEnd < json.length()
                    && json.charAt(valueEnd) != '"'
                    && json.charAt(valueEnd) != ','
                    && json.charAt(valueEnd) != '}') {
                valueEnd++;
            }

            return json.substring(valueStart, valueEnd);

        } catch (Exception e) {
            logger.warn("Failed to extract JSON value for key: {}", key, e);
            return null;
        }
    }

    /** 处理监听循环中的异常 */
    private void handleListenLoopException(Exception e) {
        logger.error("Exception in listen loop, attempting to recover", e);

        // 检查是否为致命错误
        if (isFatalException(e)) {
            logger.error("Fatal exception detected, stopping database listener", e);
            running.set(false);
            return;
        }

        // 尝试恢复连接
        try {
            Thread.sleep(1000);

            if (pgConnection == null || pgConnection.isClosed()) {
                logger.warn("Connection is closed, attempting to reconnect");
                reconnect();
            }

        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            logger.warn("Recovery interrupted");
        } catch (Exception ex) {
            logger.error("Failed to recover from exception", ex);
        }
    }

    /** 检查是否为致命异常 */
    private boolean isFatalException(Exception e) {
        // 检查cause是否为OutOfMemoryError等严重错误
        Throwable cause = e.getCause();
        while (cause != null) {
            if (cause instanceof OutOfMemoryError) {
                return true;
            }
            cause = cause.getCause();
        }

        return false;
    }

    /** 重新连接数据库 */
    private void reconnect() {
        try {
            // 关闭旧连接
            if (pgConnection != null && !pgConnection.isClosed()) {
                try {
                    pgConnection.close();
                } catch (Exception e) {
                    logger.warn("Error closing old connection", e);
                }
            }

            // 创建新连接
            pgConnection = dataSource.getConnection().unwrap(PgConnection.class);
            pgConnection.setAutoCommit(true);

            for (String tableName : tableCallbackMap.keySet()) {
                String channel = buildChannelName(tableName);
                try {
                    jdbcService.executeListen(channel);
                    logger.info("Re-registered listener for channel: {}", channel);
                } catch (Exception e) {
                    logger.warn("Error re-registering listener for channel: {}", channel, e);
                }
            }

            logger.info("Successfully reconnected to database");

        } catch (SQLException e) {
            logger.error("Failed to reconnect to database", e);
        }
    }
}
