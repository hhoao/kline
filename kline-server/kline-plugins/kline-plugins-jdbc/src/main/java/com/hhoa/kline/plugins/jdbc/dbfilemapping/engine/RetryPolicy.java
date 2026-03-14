package com.hhoa.kline.plugins.jdbc.dbfilemapping.engine;

import java.sql.SQLException;
import java.util.concurrent.Callable;
import java.util.function.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 重试策略工具类 Retry policy utility with exponential backoff for handling temporary errors
 *
 * <p>Requirements: 5.4, 10.1, 10.2
 */
public class RetryPolicy {

    private static final Logger logger = LoggerFactory.getLogger(RetryPolicy.class);

    private final int maxAttempts;
    private final long initialDelayMillis;
    private final double backoffMultiplier;
    private final long maxDelayMillis;

    /** 默认重试策略：最多3次，初始延迟100ms，指数退避2倍，最大延迟5秒 */
    public RetryPolicy() {
        this(3, 100, 2.0, 5000);
    }

    /**
     * 自定义重试策略
     *
     * @param maxAttempts 最大尝试次数
     * @param initialDelayMillis 初始延迟（毫秒）
     * @param backoffMultiplier 退避倍数
     * @param maxDelayMillis 最大延迟（毫秒）
     */
    public RetryPolicy(
            int maxAttempts,
            long initialDelayMillis,
            double backoffMultiplier,
            long maxDelayMillis) {
        if (maxAttempts <= 0) {
            throw new IllegalArgumentException("maxAttempts must be positive");
        }
        if (initialDelayMillis < 0) {
            throw new IllegalArgumentException("initialDelayMillis cannot be negative");
        }
        if (backoffMultiplier < 1.0) {
            throw new IllegalArgumentException("backoffMultiplier must be >= 1.0");
        }
        if (maxDelayMillis < initialDelayMillis) {
            throw new IllegalArgumentException("maxDelayMillis must be >= initialDelayMillis");
        }

        this.maxAttempts = maxAttempts;
        this.initialDelayMillis = initialDelayMillis;
        this.backoffMultiplier = backoffMultiplier;
        this.maxDelayMillis = maxDelayMillis;
    }

    /**
     * 执行操作，遇到可重试错误时自动重试
     *
     * @param operation 要执行的操作
     * @param <T> 返回类型
     * @return 操作结果
     * @throws Exception 如果所有重试都失败
     */
    public <T> T execute(Callable<T> operation) throws Exception {
        return execute(operation, this::isRetryableException);
    }

    /**
     * 执行操作，使用自定义的可重试判断逻辑
     *
     * @param operation 要执行的操作
     * @param retryablePredicate 判断异常是否可重试的谓词
     * @param <T> 返回类型
     * @return 操作结果
     * @throws Exception 如果所有重试都失败
     */
    public <T> T execute(Callable<T> operation, Predicate<Exception> retryablePredicate)
            throws Exception {
        Exception lastException = null;
        long delay = initialDelayMillis;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return operation.call();
            } catch (Exception e) {
                lastException = e;

                // 检查是否可重试
                if (!retryablePredicate.test(e)) {
                    logger.debug(
                            "Exception is not retryable, failing immediately: {}", e.getMessage());
                    throw e;
                }

                // 如果是最后一次尝试，不再重试
                if (attempt >= maxAttempts) {
                    logger.warn("Max retry attempts ({}) reached, giving up", maxAttempts);
                    break;
                }

                // 记录重试日志
                logger.warn(
                        "Operation failed (attempt {}/{}), retrying after {}ms: {}",
                        attempt,
                        maxAttempts,
                        delay,
                        e.getMessage());

                // 等待后重试
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new Exception("Retry interrupted", ie);
                }

                // 计算下次延迟（指数退避）
                delay = Math.min((long) (delay * backoffMultiplier), maxDelayMillis);
            }
        }

        // 所有重试都失败
        throw lastException;
    }

    /**
     * 执行无返回值的操作
     *
     * @param operation 要执行的操作
     * @throws Exception 如果所有重试都失败
     */
    public void executeVoid(VoidCallable operation) throws Exception {
        execute(
                () -> {
                    operation.call();
                    return null;
                });
    }

    /**
     * 执行无返回值的操作，使用自定义的可重试判断逻辑
     *
     * @param operation 要执行的操作
     * @param retryablePredicate 判断异常是否可重试的谓词
     * @throws Exception 如果所有重试都失败
     */
    public void executeVoid(VoidCallable operation, Predicate<Exception> retryablePredicate)
            throws Exception {
        execute(
                () -> {
                    operation.call();
                    return null;
                },
                retryablePredicate);
    }

    /**
     * 判断异常是否可重试 可重试的异常包括： - 临时数据库连接错误 - 网络超时 - 临时文件系统问题
     *
     * <p>不可重试的异常包括： - 配置错误 - 数据验证错误 - 权限错误
     */
    private boolean isRetryableException(Exception e) {
        // 数据库相关的临时错误
        if (e instanceof SQLException) {
            SQLException sqlEx = (SQLException) e;
            String sqlState = sqlEx.getSQLState();

            // PostgreSQL临时错误代码
            // 08xxx - 连接异常
            // 40xxx - 事务回滚（死锁等）
            // 53xxx - 资源不足
            // 57xxx - 操作中断
            if (sqlState != null
                    && (sqlState.startsWith("08")
                            || // Connection exception
                            sqlState.startsWith("40")
                            || // Transaction rollback
                            sqlState.startsWith("53")
                            || // Insufficient resources
                            sqlState.startsWith("57") // Operator intervention
                    )) {
                return true;
            }

            // 检查错误消息
            String message = e.getMessage();
            if (message != null) {
                message = message.toLowerCase();
                if (message.contains("connection")
                        || message.contains("timeout")
                        || message.contains("deadlock")
                        || message.contains("lock")
                        || message.contains("busy")) {
                    return true;
                }
            }
        }

        // 文件系统临时错误
        if (e instanceof java.io.IOException) {
            String message = e.getMessage();
            if (message != null) {
                message = message.toLowerCase();
                // 文件被锁定、临时不可用等
                if (message.contains("locked")
                        || message.contains("in use")
                        || message.contains("temporarily unavailable")) {
                    return true;
                }
            }
        }

        // 网络相关错误
        if (e instanceof java.net.SocketTimeoutException
                || e instanceof java.net.ConnectException) {
            return true;
        }

        // 检查cause
        Throwable cause = e.getCause();
        if (cause instanceof Exception) {
            return isRetryableException((Exception) cause);
        }

        return false;
    }

    /** 无返回值的可调用接口 */
    @FunctionalInterface
    public interface VoidCallable {
        void call() throws Exception;
    }
}
