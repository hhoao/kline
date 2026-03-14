package com.hhoa.kline.plugins.jdbc.dbfilemapping.engine;

import com.hhoa.kline.plugins.jdbc.dbfilemapping.listener.DatabaseChangeListener;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.listener.DatabaseListenerException;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.model.MappingConfiguration;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.watcher.FileWatcher;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.watcher.FileWatcherException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 恢复管理器 Manages automatic recovery for database connections and file watchers
 *
 * <p>Requirements: 10.1, 10.2
 */
public class RecoveryManager {

    private static final Logger logger = LoggerFactory.getLogger(RecoveryManager.class);

    private final DataSource dataSource;
    private final FileWatcher fileWatcher;
    private final DatabaseChangeListener databaseChangeListener;

    private final ScheduledExecutorService recoveryScheduler;
    private final AtomicBoolean recovering = new AtomicBoolean(false);

    /** 数据库连接检查间隔（秒） */
    private static final int DB_CHECK_INTERVAL_SECONDS = 30;

    /** 文件监听器检查间隔（秒） */
    private static final int WATCHER_CHECK_INTERVAL_SECONDS = 10;

    /** 最大恢复尝试次数 */
    private static final int MAX_RECOVERY_ATTEMPTS = 5;

    public RecoveryManager(
            DataSource dataSource,
            FileWatcher fileWatcher,
            DatabaseChangeListener databaseChangeListener) {
        this.dataSource = dataSource;
        this.fileWatcher = fileWatcher;
        this.databaseChangeListener = databaseChangeListener;
        this.recoveryScheduler =
                Executors.newScheduledThreadPool(
                        2,
                        r -> {
                            Thread thread = new Thread(r, "RecoveryManager-Thread");
                            thread.setDaemon(true);
                            return thread;
                        });
    }

    /** 启动自动恢复监控 */
    public void startRecoveryMonitoring() {
        logger.info("Starting recovery monitoring");

        // 定期检查数据库连接
        recoveryScheduler.scheduleWithFixedDelay(
                this::checkAndRecoverDatabaseConnection,
                DB_CHECK_INTERVAL_SECONDS,
                DB_CHECK_INTERVAL_SECONDS,
                TimeUnit.SECONDS);

        // 定期检查文件监听器
        recoveryScheduler.scheduleWithFixedDelay(
                this::checkAndRecoverFileWatcher,
                WATCHER_CHECK_INTERVAL_SECONDS,
                WATCHER_CHECK_INTERVAL_SECONDS,
                TimeUnit.SECONDS);

        logger.info("Recovery monitoring started");
    }

    /** 停止自动恢复监控 */
    public void stopRecoveryMonitoring() {
        logger.info("Stopping recovery monitoring");
        recoveryScheduler.shutdown();
        try {
            if (!recoveryScheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                recoveryScheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            recoveryScheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        logger.info("Recovery monitoring stopped");
    }

    /** 检查并恢复数据库连接 */
    private void checkAndRecoverDatabaseConnection() {
        if (recovering.get()) {
            return; // 已经在恢复中
        }

        try {
            // 测试数据库连接
            if (!isDatabaseConnectionHealthy()) {
                logger.warn("Database connection is unhealthy, attempting recovery");
                recoverDatabaseConnection();
            }
        } catch (Exception e) {
            logger.error("Error during database connection check", e);
        }
    }

    /** 检查数据库连接是否健康 */
    private boolean isDatabaseConnectionHealthy() {
        try (Connection conn = dataSource.getConnection()) {
            return conn.isValid(5); // 5秒超时
        } catch (SQLException e) {
            logger.debug("Database connection check failed: {}", e.getMessage());
            return false;
        }
    }

    /** 恢复数据库连接 */
    private void recoverDatabaseConnection() {
        if (!recovering.compareAndSet(false, true)) {
            return; // 已经在恢复中
        }

        try {
            logger.info("Starting database connection recovery");

            int attempts = 0;
            boolean recovered = false;

            while (attempts < MAX_RECOVERY_ATTEMPTS && !recovered) {
                attempts++;

                try {
                    // 等待一段时间后重试
                    if (attempts > 1) {
                        long delay = (long) Math.pow(2, attempts - 1) * 1000; // 指数退避
                        Thread.sleep(Math.min(delay, 30000)); // 最多等待30秒
                    }

                    // 尝试重新连接
                    if (isDatabaseConnectionHealthy()) {
                        logger.info("Database connection recovered after {} attempts", attempts);

                        // 重启数据库监听器
                        try {
                            databaseChangeListener.stop();
                            databaseChangeListener.start();
                            logger.info("Database listener restarted successfully");
                        } catch (DatabaseListenerException e) {
                            logger.error("Failed to restart database listener", e);
                        }

                        recovered = true;
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.warn("Database recovery interrupted");
                    break;
                } catch (Exception e) {
                    logger.warn(
                            "Database recovery attempt {} failed: {}", attempts, e.getMessage());
                }
            }

            if (!recovered) {
                logger.error(
                        "Failed to recover database connection after {} attempts",
                        MAX_RECOVERY_ATTEMPTS);
            }

        } finally {
            recovering.set(false);
        }
    }

    /** 检查并恢复文件监听器 */
    private void checkAndRecoverFileWatcher() {
        if (recovering.get()) {
            return; // 已经在恢复中
        }

        try {
            // 检查文件监听器是否运行
            if (!fileWatcher.isRunning()) {
                logger.warn("FileWatcher is not running, attempting recovery");
                recoverFileWatcher();
            }
        } catch (Exception e) {
            logger.error("Error during file watcher check", e);
        }
    }

    /** 恢复文件监听器 */
    private void recoverFileWatcher() {
        if (!recovering.compareAndSet(false, true)) {
            return; // 已经在恢复中
        }

        try {
            logger.info("Starting file watcher recovery");

            int attempts = 0;
            boolean recovered = false;

            while (attempts < MAX_RECOVERY_ATTEMPTS && !recovered) {
                attempts++;

                try {
                    // 等待一段时间后重试
                    if (attempts > 1) {
                        long delay = (long) Math.pow(2, attempts - 1) * 1000; // 指数退避
                        Thread.sleep(Math.min(delay, 30000)); // 最多等待30秒
                    }

                    // 尝试重启文件监听器
                    fileWatcher.start();

                    if (fileWatcher.isRunning()) {
                        logger.info("FileWatcher recovered after {} attempts", attempts);
                        recovered = true;
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.warn("File watcher recovery interrupted");
                    break;
                } catch (FileWatcherException e) {
                    logger.warn(
                            "File watcher recovery attempt {} failed: {}",
                            attempts,
                            e.getMessage());
                }
            }

            if (!recovered) {
                logger.error(
                        "Failed to recover file watcher after {} attempts", MAX_RECOVERY_ATTEMPTS);
            }

        } finally {
            recovering.set(false);
        }
    }

    /**
     * 尝试恢复指定目录的监听
     *
     * @param directory 目录路径
     * @param config 映射配置
     * @return 是否成功恢复
     */
    public boolean recoverDirectoryWatch(Path directory, MappingConfiguration config) {
        logger.info("Attempting to recover directory watch: {}", directory);

        try {
            // 检查目录是否存在
            if (!Files.exists(directory)) {
                logger.warn("Directory does not exist, cannot recover: {}", directory);
                return false;
            }

            // 尝试重新注册监听
            fileWatcher.unregisterDirectory(directory);
            fileWatcher.registerDirectory(directory, config);

            logger.info("Successfully recovered directory watch: {}", directory);
            return true;

        } catch (FileWatcherException e) {
            logger.error("Failed to recover directory watch: {}", directory, e);
            return false;
        }
    }

    /**
     * 检查目录是否可访问
     *
     * @param directory 目录路径
     * @return 是否可访问
     */
    public boolean isDirectoryAccessible(Path directory) {
        return Files.exists(directory)
                && Files.isDirectory(directory)
                && Files.isReadable(directory)
                && Files.isWritable(directory);
    }
}
