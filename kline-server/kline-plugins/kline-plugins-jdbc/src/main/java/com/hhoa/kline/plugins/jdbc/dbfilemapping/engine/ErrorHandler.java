package com.hhoa.kline.plugins.jdbc.dbfilemapping.engine;

import com.hhoa.kline.plugins.jdbc.dbfilemapping.enums.ErrorType;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.logger.SyncLogger;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.model.SyncError;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.serializer.SerializationException;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.validator.ValidationException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 错误处理器 Centralized error handler for categorizing and handling different types of errors
 *
 * <p>Requirements: 5.4, 10.1, 10.2
 */
public class ErrorHandler {

    private static final Logger logger = LoggerFactory.getLogger(ErrorHandler.class);

    private final SyncLogger syncLogger;
    private final RetryPolicy retryPolicy;

    public ErrorHandler(SyncLogger syncLogger) {
        this.syncLogger = syncLogger;
        this.retryPolicy = new RetryPolicy();
    }

    public ErrorHandler(SyncLogger syncLogger, RetryPolicy retryPolicy) {
        this.syncLogger = syncLogger;
        this.retryPolicy = retryPolicy;
    }

    /**
     * 处理同步错误 根据错误类型决定处理策略：重试、跳过或快速失败
     *
     * @param tableName 表名
     * @param filePath 文件路径（可选）
     * @param exception 异常
     * @return 错误处理结果
     */
    public ErrorHandlingResult handleSyncError(
            String tableName, Path filePath, Exception exception) {
        ErrorType errorType = categorizeError(exception);
        String filePathStr = filePath != null ? filePath.toString() : null;

        // 记录错误
        SyncError syncError = createSyncError(tableName, filePathStr, errorType, exception);
        syncLogger.logError(syncError);

        // 根据错误类型决定处理策略
        ErrorHandlingStrategy strategy = determineStrategy(errorType, exception);

        logger.debug(
                "Error categorized as {} with strategy {}: {}",
                errorType,
                strategy,
                exception.getMessage());

        return new ErrorHandlingResult(errorType, strategy, syncError);
    }

    /** 分类错误类型 */
    private ErrorType categorizeError(Exception exception) {
        // 文件读写错误
        if (exception instanceof IOException) {
            String message = exception.getMessage();
            if (message != null && message.toLowerCase().contains("read")) {
                return ErrorType.FILE_READ_ERROR;
            }
            return ErrorType.FILE_WRITE_ERROR;
        }

        // JSON解析错误
        if (exception instanceof SerializationException) {
            return ErrorType.JSON_PARSE_ERROR;
        }

        // 数据库错误
        if (exception instanceof SQLException || (exception.getCause() instanceof SQLException)) {
            return ErrorType.DATABASE_ERROR;
        }

        // 验证错误
        if (exception instanceof ValidationException
                || exception.getMessage() != null
                        && exception.getMessage().contains("validation")) {
            return ErrorType.VALIDATION_ERROR;
        }

        // 冲突错误
        if (exception instanceof ConflictException) {
            return ErrorType.CONFLICT_ERROR;
        }

        // 默认为数据库错误
        return ErrorType.DATABASE_ERROR;
    }

    /** 确定错误处理策略 */
    private ErrorHandlingStrategy determineStrategy(ErrorType errorType, Exception exception) {
        switch (errorType) {
            case DATABASE_ERROR:
                // 数据库错误：如果是临时错误则重试，否则快速失败
                try {
                    return retryPolicy.execute(() -> true, e -> false) != null
                            ? ErrorHandlingStrategy.RETRY
                            : ErrorHandlingStrategy.FAIL_FAST;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

            case FILE_READ_ERROR:
            case FILE_WRITE_ERROR:
                // 文件系统错误：临时错误重试，权限错误快速失败
                if (isPermissionError(exception)) {
                    return ErrorHandlingStrategy.FAIL_FAST;
                }
                return ErrorHandlingStrategy.RETRY;

            case JSON_PARSE_ERROR:
            case VALIDATION_ERROR:
                // 数据错误：跳过并记录
                return ErrorHandlingStrategy.SKIP_AND_LOG;

            case CONFLICT_ERROR:
                // 冲突错误：跳过并记录（已由ConflictResolver处理）
                return ErrorHandlingStrategy.SKIP_AND_LOG;

            default:
                return ErrorHandlingStrategy.SKIP_AND_LOG;
        }
    }

    /** 检查是否为权限错误 */
    private boolean isPermissionError(Exception exception) {
        String message = exception.getMessage();
        if (message != null) {
            message = message.toLowerCase();
            return message.contains("permission")
                    || message.contains("access denied")
                    || message.contains("forbidden");
        }
        return false;
    }

    /** 创建同步错误对象 */
    private SyncError createSyncError(
            String tableName, String filePath, ErrorType errorType, Exception exception) {
        SyncError error = new SyncError();
        error.setTimestamp(LocalDateTime.now());
        error.setTableName(tableName);
        error.setFilePath(filePath);
        error.setErrorType(errorType);
        error.setErrorMessage(exception.getMessage());
        error.setStackTrace(getStackTraceAsString(exception));
        return error;
    }

    /** 获取异常堆栈跟踪字符串 */
    private String getStackTraceAsString(Exception exception) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        exception.printStackTrace(pw);
        return sw.toString();
    }

    /**
     * 使用重试策略执行操作
     *
     * @param operation 要执行的操作
     * @param <T> 返回类型
     * @return 操作结果
     * @throws Exception 如果所有重试都失败
     */
    public <T> T executeWithRetry(RetryPolicy.VoidCallable operation) throws Exception {
        retryPolicy.executeVoid(operation);
        return null;
    }

    /** 错误处理策略枚举 */
    public enum ErrorHandlingStrategy {
        /** 重试：用于临时错误（数据库连接失败、文件锁定等） */
        RETRY,

        /** 跳过并记录：用于数据错误（JSON格式错误、验证失败等） */
        SKIP_AND_LOG,

        /** 快速失败：用于配置错误、权限错误等不可恢复的错误 */
        FAIL_FAST
    }

    /** 错误处理结果 */
    @lombok.Value
    public static class ErrorHandlingResult {
        ErrorType errorType;
        ErrorHandlingStrategy strategy;
        SyncError syncError;

        public boolean shouldRetry() {
            return strategy == ErrorHandlingStrategy.RETRY;
        }

        public boolean shouldSkip() {
            return strategy == ErrorHandlingStrategy.SKIP_AND_LOG;
        }

        public boolean shouldFailFast() {
            return strategy == ErrorHandlingStrategy.FAIL_FAST;
        }
    }
}
