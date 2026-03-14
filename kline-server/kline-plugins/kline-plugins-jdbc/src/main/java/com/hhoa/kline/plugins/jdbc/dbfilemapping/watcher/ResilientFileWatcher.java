package com.hhoa.kline.plugins.jdbc.dbfilemapping.watcher;

import com.hhoa.kline.plugins.jdbc.dbfilemapping.model.MappingConfiguration;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 弹性文件监听器 Resilient file watcher that gracefully degrades from WatchService to polling Implements
 * graceful degradation strategy
 *
 * <p>Requirements: 10.1, 10.2
 */
public class ResilientFileWatcher implements FileWatcher {

    private static final Logger logger = LoggerFactory.getLogger(ResilientFileWatcher.class);

    private FileWatcher activeWatcher;
    private final FileWatcher primaryWatcher;
    private final FileWatcher fallbackWatcher;

    private boolean usingFallback = false;

    /** 创建弹性文件监听器 默认使用DefaultFileWatcher作为主监听器，PollingFileWatcher作为备用 */
    public ResilientFileWatcher() {
        this.primaryWatcher = new DefaultFileWatcher();
        this.fallbackWatcher = new PollingFileWatcher();
        this.activeWatcher = primaryWatcher;
    }

    /**
     * 创建弹性文件监听器，使用自定义的主监听器和备用监听器
     *
     * @param primaryWatcher 主监听器
     * @param fallbackWatcher 备用监听器
     */
    public ResilientFileWatcher(FileWatcher primaryWatcher, FileWatcher fallbackWatcher) {
        this.primaryWatcher = primaryWatcher;
        this.fallbackWatcher = fallbackWatcher;
        this.activeWatcher = primaryWatcher;
    }

    @Override
    public void registerDirectory(Path directory, MappingConfiguration config)
            throws FileWatcherException {
        try {
            activeWatcher.registerDirectory(directory, config);
        } catch (FileWatcherException e) {
            logger.warn("Failed to register directory with active watcher, attempting fallback", e);
            degradeToFallback();
            activeWatcher.registerDirectory(directory, config);
        }
    }

    @Override
    public void unregisterDirectory(Path directory) {
        activeWatcher.unregisterDirectory(directory);
    }

    @Override
    public void start() throws FileWatcherException {
        try {
            activeWatcher.start();

            if (usingFallback) {
                logger.warn("File watcher started in degraded mode (using polling)");
            } else {
                logger.info("File watcher started successfully (using WatchService)");
            }

        } catch (FileWatcherException e) {
            logger.error("Failed to start active watcher, attempting fallback", e);
            degradeToFallback();
            activeWatcher.start();
            logger.warn("File watcher started in degraded mode (using polling) after failure");
        }
    }

    @Override
    public void stop() {
        activeWatcher.stop();

        if (usingFallback) {
            logger.info("File watcher stopped (was using polling)");
        } else {
            logger.info("File watcher stopped (was using WatchService)");
        }
    }

    @Override
    public void setFileChangeCallback(FileChangeCallback callback) {
        // 为两个监听器都设置回调
        primaryWatcher.setFileChangeCallback(callback);
        fallbackWatcher.setFileChangeCallback(callback);
    }

    @Override
    public boolean isRunning() {
        return activeWatcher.isRunning();
    }

    /** 降级到备用监听器 Gracefully degrade to fallback watcher */
    private void degradeToFallback() {
        if (usingFallback) {
            logger.warn("Already using fallback watcher");
            return;
        }

        logger.warn("Degrading to fallback file watcher (polling mode)");

        try {
            // 停止主监听器
            if (primaryWatcher.isRunning()) {
                primaryWatcher.stop();
            }
        } catch (Exception e) {
            logger.error("Error stopping primary watcher during degradation", e);
        }

        // 切换到备用监听器
        activeWatcher = fallbackWatcher;
        usingFallback = true;

        logger.info("Successfully degraded to fallback file watcher");
    }

    /**
     * 尝试恢复到主监听器 Attempt to recover to primary watcher
     *
     * @return 是否成功恢复
     */
    public boolean attemptRecovery() {
        if (!usingFallback) {
            logger.debug("Not using fallback, no recovery needed");
            return true;
        }

        logger.info("Attempting to recover to primary file watcher");

        try {
            // 测试主监听器是否可用
            // 这里可以添加更复杂的健康检查逻辑

            // 停止备用监听器
            if (fallbackWatcher.isRunning()) {
                fallbackWatcher.stop();
            }

            // 切换回主监听器
            activeWatcher = primaryWatcher;
            usingFallback = false;

            // 启动主监听器
            primaryWatcher.start();

            logger.info("Successfully recovered to primary file watcher");
            return true;

        } catch (Exception e) {
            logger.error("Failed to recover to primary watcher, staying with fallback", e);

            // 恢复失败，切换回备用监听器
            activeWatcher = fallbackWatcher;
            usingFallback = true;

            try {
                if (!fallbackWatcher.isRunning()) {
                    fallbackWatcher.start();
                }
            } catch (FileWatcherException ex) {
                logger.error("Failed to restart fallback watcher after recovery failure", ex);
            }

            return false;
        }
    }

    /**
     * 检查是否正在使用备用监听器
     *
     * @return true if using fallback (polling), false if using primary (WatchService)
     */
    public boolean isUsingFallback() {
        return usingFallback;
    }

    /**
     * 获取当前活动的监听器类型
     *
     * @return 监听器类型描述
     */
    public String getActiveWatcherType() {
        if (usingFallback) {
            return "Polling (Degraded Mode)";
        } else {
            return "WatchService (Normal Mode)";
        }
    }
}
