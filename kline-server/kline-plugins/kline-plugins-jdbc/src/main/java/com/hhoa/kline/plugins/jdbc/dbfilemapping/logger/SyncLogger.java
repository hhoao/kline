package com.hhoa.kline.plugins.jdbc.dbfilemapping.logger;

import com.hhoa.kline.plugins.jdbc.dbfilemapping.enums.ErrorType;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.model.SyncError;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 同步日志记录器 Handles structured logging for synchronization operations
 *
 * <p>Requirements: 7.1, 7.2, 7.3
 */
public class SyncLogger {

    private static final Logger logger = LoggerFactory.getLogger(SyncLogger.class);
    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    /**
     * 记录操作日志 Logs a synchronization operation with type, timestamp, and result
     *
     * @param operationType 操作类型 (e.g., "INIT_SYNC", "FILE_TO_DB", "DB_TO_FILE")
     * @param tableName 表名
     * @param filePath 文件路径 (可选)
     * @param success 是否成功
     * @param message 附加消息
     */
    public void logOperation(
            String operationType,
            String tableName,
            String filePath,
            boolean success,
            String message) {
        Map<String, Object> logData = new HashMap<>();
        logData.put("timestamp", LocalDateTime.now().format(TIMESTAMP_FORMATTER));
        logData.put("operationType", operationType);
        logData.put("tableName", tableName);
        logData.put("filePath", filePath);
        logData.put("success", success);
        logData.put("message", message);

        String logMessage = formatLogMessage(logData);

        if (success) {
            logger.info(logMessage);
        } else {
            logger.warn(logMessage);
        }
    }

    /** 记录操作日志（简化版本，不包含文件路径） */
    public void logOperation(
            String operationType, String tableName, boolean success, String message) {
        logOperation(operationType, tableName, null, success, message);
    }

    /**
     * 记录错误日志 Logs detailed error information including message and stack trace
     *
     * @param syncError 同步错误对象
     */
    public void logError(SyncError syncError) {
        Map<String, Object> logData = new HashMap<>();
        logData.put("timestamp", syncError.getTimestamp().format(TIMESTAMP_FORMATTER));
        logData.put("tableName", syncError.getTableName());
        logData.put("filePath", syncError.getFilePath());
        logData.put("errorType", syncError.getErrorType());
        logData.put("errorMessage", syncError.getErrorMessage());

        String logMessage = formatLogMessage(logData);

        if (syncError.getStackTrace() != null && !syncError.getStackTrace().isEmpty()) {
            logger.error(logMessage + "\nStackTrace:\n" + syncError.getStackTrace());
        } else {
            logger.error(logMessage);
        }
    }

    /**
     * 记录错误日志（从异常创建） Logs error from exception with automatic stack trace extraction
     *
     * @param tableName 表名
     * @param filePath 文件路径
     * @param errorType 错误类型
     * @param exception 异常对象
     */
    public void logError(
            String tableName, String filePath, ErrorType errorType, Exception exception) {
        String stackTrace = getStackTraceAsString(exception);
        SyncError syncError =
                new SyncError(tableName, filePath, errorType, exception.getMessage(), stackTrace);
        logError(syncError);
    }

    /** 记录错误日志（简化版本） */
    public void logError(String tableName, ErrorType errorType, String errorMessage) {
        SyncError syncError = new SyncError(tableName, null, errorType, errorMessage);
        logError(syncError);
    }

    /** 记录初始化开始 */
    public void logInitializationStart(String tableName, long totalRecords) {
        logOperation(
                "INIT_SYNC_START",
                tableName,
                true,
                String.format(
                        "Starting initialization for table %s with %d records",
                        tableName, totalRecords));
    }

    /** 记录初始化完成 */
    public void logInitializationComplete(
            String tableName, long syncedRecords, long failedRecords, long durationMs) {
        logOperation(
                "INIT_SYNC_COMPLETE",
                tableName,
                true,
                String.format(
                        "Initialization complete: synced=%d, failed=%d, duration=%dms",
                        syncedRecords, failedRecords, durationMs));
    }

    /** 记录文件到数据库同步 */
    public void logFileToDatabase(
            String tableName, String filePath, boolean success, String message) {
        logOperation("FILE_TO_DB", tableName, filePath, success, message);
    }

    /** 记录数据库到文件同步 */
    public void logDatabaseToFile(
            String tableName, String filePath, boolean success, String message) {
        logOperation("DB_TO_FILE", tableName, filePath, success, message);
    }

    /** 记录文件删除操作 */
    public void logFileDeletion(
            String tableName, String filePath, boolean success, String message) {
        logOperation("FILE_DELETE", tableName, filePath, success, message);
    }

    /** 记录冲突检测 */
    public void logConflictDetected(String tableName, String filePath, String conflictStrategy) {
        logOperation(
                "CONFLICT_DETECTED",
                tableName,
                filePath,
                true,
                String.format("Conflict detected, using strategy: %s", conflictStrategy));
    }

    /** 记录冲突解决 */
    public void logConflictResolved(String tableName, String filePath, String resolution) {
        logOperation(
                "CONFLICT_RESOLVED",
                tableName,
                filePath,
                true,
                String.format("Conflict resolved: %s", resolution));
    }

    /** 记录配置加载 */
    public void logConfigurationLoaded(int configCount) {
        logger.info("Configuration loaded successfully: {} mappings", configCount);
    }

    /** 记录配置验证失败 */
    public void logConfigurationValidationFailed(String tableName, String reason) {
        logger.error("Configuration validation failed for table {}: {}", tableName, reason);
    }

    /** 格式化日志消息 */
    private String formatLogMessage(Map<String, Object> logData) {
        StringBuilder sb = new StringBuilder();
        sb.append("[SYNC] ");

        for (Map.Entry<String, Object> entry : logData.entrySet()) {
            if (entry.getValue() != null) {
                sb.append(entry.getKey()).append("=").append(entry.getValue()).append(" ");
            }
        }

        return sb.toString().trim();
    }

    /** 将异常堆栈跟踪转换为字符串 */
    private String getStackTraceAsString(Exception exception) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        exception.printStackTrace(pw);
        return sw.toString();
    }

    /** 记录调试信息 */
    public void debug(String message) {
        logger.debug(message);
    }

    /** 记录信息 */
    public void info(String message) {
        logger.info(message);
    }

    /** 记录警告 */
    public void warn(String message) {
        logger.warn(message);
    }

    /** 记录错误 */
    public void error(String message) {
        logger.error(message);
    }

    /** 记录错误（带异常） */
    public void error(String message, Exception exception) {
        logger.error(message, exception);
    }
}
