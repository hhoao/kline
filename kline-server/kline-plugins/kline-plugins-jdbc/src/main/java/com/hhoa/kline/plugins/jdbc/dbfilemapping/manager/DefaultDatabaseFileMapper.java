package com.hhoa.kline.plugins.jdbc.dbfilemapping.manager;

import com.hhoa.kline.plugins.jdbc.dbfilemapping.engine.SyncEngine;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.engine.SyncException;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.enums.SyncState;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.listener.ChangeCallback;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.listener.DatabaseChangeListener;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.listener.DatabaseListenerException;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.logger.SyncLogger;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.model.MappingConfiguration;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.model.SyncStatus;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.model.ValidationResult;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.watcher.FileChangeCallback;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.watcher.FileWatcher;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.watcher.FileWatcherException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 数据库文件映射器默认实现 Default implementation of DatabaseFileMapper that integrates all components
 *
 * <p>Requirements: 1.1, 1.3, 7.4
 */
public class DefaultDatabaseFileMapper implements DatabaseFileMapper {

    private static final Logger logger = LoggerFactory.getLogger(DefaultDatabaseFileMapper.class);

    private final ConfigurationManager configurationManager;
    private final SyncEngine syncEngine;
    private final FileWatcher fileWatcher;
    private final DatabaseChangeListener databaseChangeListener;
    private final SyncLogger syncLogger;

    private final Map<String, SyncStatus> syncStatusMap;
    private final ReadWriteLock statusLock;
    private volatile boolean running;

    /** 关闭超时时间（秒） Timeout for graceful shutdown to avoid infinite waiting Requirements: 10.4 */
    private static final int SHUTDOWN_TIMEOUT_SECONDS = 30;

    public DefaultDatabaseFileMapper(
            ConfigurationManager configurationManager,
            SyncEngine syncEngine,
            FileWatcher fileWatcher,
            DatabaseChangeListener databaseChangeListener,
            SyncLogger syncLogger) {

        this.configurationManager = configurationManager;
        this.syncEngine = syncEngine;
        this.fileWatcher = fileWatcher;
        this.databaseChangeListener = databaseChangeListener;
        this.syncLogger = syncLogger;

        this.syncStatusMap = new ConcurrentHashMap<>();
        this.statusLock = new ReentrantReadWriteLock();
        this.running = false;
    }

    @Override
    public synchronized void initialize(List<MappingConfiguration> configurations)
            throws DatabaseFileMapperException {
        if (configurations == null || configurations.isEmpty()) {
            return;
        }

        logger.info(
                "Initializing DatabaseFileMapper with {} configurations", configurations.size());

        try {
            // 加载并验证配置
            Map<String, ValidationResult> validationResults =
                    configurationManager.loadConfigurations(configurations);

            // 检查是否有配置加载失败
            List<String> failedConfigs = new ArrayList<>();
            for (Map.Entry<String, ValidationResult> entry : validationResults.entrySet()) {
                if (!entry.getValue().isValid()) {
                    failedConfigs.add(entry.getKey());
                    syncLogger.logConfigurationValidationFailed(
                            entry.getKey(), String.join(", ", entry.getValue().getErrors()));
                }
            }

            if (!failedConfigs.isEmpty()) {
                logger.warn("Some configurations failed validation: {}", failedConfigs);
            }

            // 为每个有效配置初始化同步状态
            Collection<MappingConfiguration> validConfigs =
                    configurationManager.getAllConfigurations();
            for (MappingConfiguration config : validConfigs) {
                String key = makeStatusKey(config.getSchemaName(), config.getTableName());
                SyncStatus status =
                        new SyncStatus(config.getTableName(), config.getTargetDirectory());
                status.setState(SyncState.INITIALIZING);
                syncStatusMap.put(key, status);
            }

            syncLogger.logConfigurationLoaded(validConfigs.size());
            logger.info(
                    "DatabaseFileMapper initialized successfully with {} valid configurations",
                    validConfigs.size());

        } catch (Exception e) {
            logger.error("Failed to initialize DatabaseFileMapper", e);
            throw new DatabaseFileMapperException("Initialization failed", e);
        }
    }

    @Override
    public synchronized void startAll() throws DatabaseFileMapperException {
        if (running) {
            logger.warn("DatabaseFileMapper is already running");
            return;
        }

        logger.info("Starting all mappings");

        try {
            Collection<MappingConfiguration> configurations =
                    configurationManager.getAllConfigurations();

            // 启动数据库监听器
            databaseChangeListener.start();
            logger.info("DatabaseChangeListener started");

            // 为每个配置启动同步
            for (MappingConfiguration config : configurations) {
                try {
                    startMapping(config);
                } catch (Exception e) {
                    logger.error(
                            "Failed to start mapping for table {}",
                            config.getQualifiedTableName(),
                            e);
                    updateSyncStatus(config, SyncState.ERROR);
                    // 继续启动其他映射
                }
            }

            // 设置文件变更回调
            fileWatcher.setFileChangeCallback(new FileChangeCallbackImpl());

            // 启动文件监听器
            fileWatcher.start();
            logger.info("FileWatcher started");

            running = true;
            logger.info("All mappings started successfully");

        } catch (FileWatcherException e) {
            logger.error("Failed to start FileWatcher", e);
            throw new DatabaseFileMapperException("Failed to start file watcher", e);
        } catch (DatabaseListenerException e) {
            logger.error("Failed to start DatabaseChangeListener", e);
            throw new DatabaseFileMapperException("Failed to start database listener", e);
        } catch (Exception e) {
            logger.error("Failed to start all mappings", e);
            throw new DatabaseFileMapperException("Failed to start mappings", e);
        }
    }

    /** 启动单个映射 */
    private void startMapping(MappingConfiguration config)
            throws SyncException, FileWatcherException, DatabaseListenerException {
        logger.info("Starting mapping for table: {}", config.getQualifiedTableName());

        // 更新状态为同步中
        updateSyncStatus(config, SyncState.SYNCING);

        // 执行初始化同步（数据库 → 文件）
        syncEngine.initializeSync(config);

        // 注册文件监听
        Path targetDir =
                Paths.get(config.getTargetDirectory())
                        .resolve(config.getSchemaName())
                        .resolve(config.getTableName());
        fileWatcher.registerDirectory(targetDir, config);
        logger.info("Registered file watcher for directory: {}", targetDir);

        // 注册数据库监听
        if (config.isEnableRealTimeSync()) {
            databaseChangeListener.listenToTable(
                    config.getQualifiedTableName(), new DatabaseChangeCallbackImpl(config));
            logger.info(
                    "Registered database listener for table: {}", config.getQualifiedTableName());
        }

        // 更新状态为空闲
        updateSyncStatus(config, SyncState.IDLE);
        updateLastSyncTime(config);

        logger.info("Mapping started successfully for table: {}", config.getQualifiedTableName());
    }

    @Override
    public synchronized void stopAll() {
        if (!running) {
            logger.warn("DatabaseFileMapper is not running");
            return;
        }

        logger.info("Stopping all mappings - initiating graceful shutdown...");

        // 创建一个ExecutorService来并行关闭组件，并设置总体超时
        // Create an ExecutorService to shutdown components in parallel with overall timeout
        ExecutorService shutdownExecutor = Executors.newFixedThreadPool(2);

        try {
            // 标记为正在关闭
            // Mark as shutting down
            running = false;

            // 更新所有状态为已停止
            // Update all statuses to STOPPED
            updateAllStatesToStopped();

            // 并行关闭FileWatcher和DatabaseChangeListener
            // Shutdown FileWatcher and DatabaseChangeListener in parallel
            Future<Void> fileWatcherFuture =
                    shutdownExecutor.submit(
                            () -> {
                                try {
                                    logger.debug("Stopping FileWatcher...");
                                    fileWatcher.stop();
                                    logger.info("FileWatcher stopped successfully");
                                } catch (Exception e) {
                                    logger.error("Error stopping FileWatcher", e);
                                }
                                return null;
                            });

            Future<Void> dbListenerFuture =
                    shutdownExecutor.submit(
                            () -> {
                                try {
                                    logger.debug("Stopping DatabaseChangeListener...");
                                    databaseChangeListener.stop();
                                    logger.info("DatabaseChangeListener stopped successfully");
                                } catch (Exception e) {
                                    logger.error("Error stopping DatabaseChangeListener", e);
                                }
                                return null;
                            });

            // 等待两个组件都关闭完成，设置总体超时
            // Wait for both components to shutdown with overall timeout
            try {
                fileWatcherFuture.get(SHUTDOWN_TIMEOUT_SECONDS / 2, TimeUnit.SECONDS);
                logger.debug("FileWatcher shutdown completed");
            } catch (TimeoutException e) {
                logger.warn(
                        "FileWatcher shutdown timed out after {} seconds",
                        SHUTDOWN_TIMEOUT_SECONDS / 2);
                fileWatcherFuture.cancel(true);
            } catch (Exception e) {
                logger.error("Error waiting for FileWatcher shutdown", e);
            }

            try {
                dbListenerFuture.get(SHUTDOWN_TIMEOUT_SECONDS / 2, TimeUnit.SECONDS);
                logger.debug("DatabaseChangeListener shutdown completed");
            } catch (TimeoutException e) {
                logger.warn(
                        "DatabaseChangeListener shutdown timed out after {} seconds",
                        SHUTDOWN_TIMEOUT_SECONDS / 2);
                dbListenerFuture.cancel(true);
            } catch (Exception e) {
                logger.error("Error waiting for DatabaseChangeListener shutdown", e);
            }

            logger.info("All mappings stopped successfully - all resources cleaned up");

        } catch (Exception e) {
            logger.error("Error during shutdown, some resources may not be cleaned up properly", e);

            // 尽力清理剩余资源
            // Best effort cleanup of remaining resources
            try {
                if (fileWatcher.isRunning()) {
                    fileWatcher.stop();
                }
                if (databaseChangeListener.isRunning()) {
                    databaseChangeListener.stop();
                }
            } catch (Exception cleanupEx) {
                logger.error("Error during cleanup after shutdown failure", cleanupEx);
            }

        } finally {
            // 关闭shutdown executor
            // Shutdown the executor used for parallel shutdown
            shutdownExecutor.shutdown();
            try {
                if (!shutdownExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    shutdownExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                shutdownExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /** 更新所有同步状态为已停止 Update all sync statuses to STOPPED state */
    private void updateAllStatesToStopped() {
        statusLock.writeLock().lock();
        try {
            for (SyncStatus status : syncStatusMap.values()) {
                status.setState(SyncState.STOPPED);
            }
            logger.debug("Updated {} sync statuses to STOPPED", syncStatusMap.size());
        } finally {
            statusLock.writeLock().unlock();
        }
    }

    @Override
    public synchronized SyncStatus getSyncStatus(String schemaName, String tableName) {
        statusLock.readLock().lock();
        try {
            String key = makeStatusKey(schemaName, tableName);
            return syncStatusMap.get(key);
        } finally {
            statusLock.readLock().unlock();
        }
    }

    @Override
    public synchronized Collection<SyncStatus> getAllSyncStatus() {
        statusLock.readLock().lock();
        try {
            return Collections.unmodifiableCollection(new ArrayList<>(syncStatusMap.values()));
        } finally {
            statusLock.readLock().unlock();
        }
    }

    @Override
    public synchronized void triggerFullSync(String schemaName, String tableName)
            throws DatabaseFileMapperException {
        logger.info("Triggering full sync for table: {}.{}", schemaName, tableName);

        // 获取配置
        MappingConfiguration config = configurationManager.getConfiguration(schemaName, tableName);
        if (config == null) {
            throw new DatabaseFileMapperException(
                    String.format(
                            "Configuration not found for table: %s.%s", schemaName, tableName));
        }

        try {
            // 更新状态
            updateSyncStatus(config, SyncState.SYNCING);

            // 执行全量同步
            syncEngine.initializeSync(config);

            // 更新状态
            updateSyncStatus(config, SyncState.IDLE);
            updateLastSyncTime(config);

            logger.info("Full sync completed for table: {}.{}", schemaName, tableName);

        } catch (SyncException e) {
            updateSyncStatus(config, SyncState.ERROR);
            logger.error("Full sync failed for table: {}.{}", schemaName, tableName, e);
            throw new DatabaseFileMapperException("Full sync failed", e);
        }
    }

    @Override
    public synchronized void reloadConfiguration(List<MappingConfiguration> configurations)
            throws DatabaseFileMapperException {
        if (configurations == null || configurations.isEmpty()) {
            return;
        }
        logger.info("Reloading configurations");

        boolean wasRunning = running;

        try {
            // 如果正在运行，先停止
            if (wasRunning) {
                stopAll();
            }

            // 清除旧的同步状态
            statusLock.writeLock().lock();
            try {
                syncStatusMap.clear();
            } finally {
                statusLock.writeLock().unlock();
            }

            // 重新加载配置
            Map<String, ValidationResult> validationResults =
                    configurationManager.reloadConfigurations(configurations);

            // 检查验证结果
            List<String> failedConfigs = new ArrayList<>();
            for (Map.Entry<String, ValidationResult> entry : validationResults.entrySet()) {
                if (!entry.getValue().isValid()) {
                    failedConfigs.add(entry.getKey());
                    syncLogger.logConfigurationValidationFailed(
                            entry.getKey(), String.join(", ", entry.getValue().getErrors()));
                }
            }

            if (!failedConfigs.isEmpty()) {
                logger.warn(
                        "Some configurations failed validation during reload: {}", failedConfigs);
            }

            // 为每个有效配置初始化同步状态
            Collection<MappingConfiguration> validConfigs =
                    configurationManager.getAllConfigurations();
            for (MappingConfiguration config : validConfigs) {
                String key = makeStatusKey(config.getSchemaName(), config.getTableName());
                SyncStatus status =
                        new SyncStatus(config.getTableName(), config.getTargetDirectory());
                status.setState(SyncState.INITIALIZING);
                syncStatusMap.put(key, status);
            }

            syncLogger.logConfigurationLoaded(validConfigs.size());

            // 如果之前在运行，重新启动
            if (wasRunning) {
                startAll();
            }

            logger.info(
                    "Configuration reloaded successfully with {} valid configurations",
                    validConfigs.size());

        } catch (Exception e) {
            logger.error("Failed to reload configuration", e);
            throw new DatabaseFileMapperException("Configuration reload failed", e);
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getConfigurationCount() {
        return configurationManager.getConfigurationCount();
    }

    // ========== Private Helper Methods ==========

    /** 创建状态键 */
    private String makeStatusKey(String schemaName, String tableName) {
        String schema = (schemaName == null || schemaName.trim().isEmpty()) ? "public" : schemaName;
        return schema + "." + tableName;
    }

    /** 更新同步状态 */
    private void updateSyncStatus(MappingConfiguration config, SyncState state) {
        statusLock.writeLock().lock();
        try {
            String key = makeStatusKey(config.getSchemaName(), config.getTableName());
            SyncStatus status = syncStatusMap.get(key);
            if (status != null) {
                status.setState(state);
            }
        } finally {
            statusLock.writeLock().unlock();
        }
    }

    /** 更新最后同步时间 */
    private void updateLastSyncTime(MappingConfiguration config) {
        statusLock.writeLock().lock();
        try {
            String key = makeStatusKey(config.getSchemaName(), config.getTableName());
            SyncStatus status = syncStatusMap.get(key);
            if (status != null) {
                status.setLastSyncTime(LocalDateTime.now());
            }
        } finally {
            statusLock.writeLock().unlock();
        }
    }

    /**
     * 根据文件路径查找配置 支持两种文件结构模式： - SINGLE_JSON: targetDirectory/schema/table/primaryKey.json -
     * FIELD_FILES: targetDirectory/schema/table/primaryKey/fieldName
     */
    private MappingConfiguration findConfigurationByPath(Path filePath) {
        if (filePath == null) {
            return null;
        }

        // 从路径中提取schema和table名称
        Path parent = filePath.getParent();
        if (parent == null) {
            return null;
        }

        // 判断是哪种模式
        String fileName = filePath.getFileName().toString();
        boolean isSingleJsonMode = fileName.endsWith(".json");

        String tableName;
        String schemaName;

        if (isSingleJsonMode) {
            // SINGLE_JSON 模式: targetDirectory/schema/table/primaryKey.json
            tableName = parent.getFileName().toString();
            Path grandParent = parent.getParent();
            if (grandParent == null) {
                return null;
            }
            schemaName = grandParent.getFileName().toString();

        } else {
            // FIELD_FILES 模式: targetDirectory/schema/table/primaryKey/fieldName
            // parent 是主键目录
            Path tableDir = parent.getParent();
            if (tableDir == null) {
                return null;
            }
            tableName = tableDir.getFileName().toString();

            Path schemaDir = tableDir.getParent();
            if (schemaDir == null) {
                return null;
            }
            schemaName = schemaDir.getFileName().toString();
        }

        return configurationManager.getConfiguration(schemaName, tableName);
    }

    // ========== Inner Classes for Callbacks ==========

    /** 文件变更回调实现 */
    private class FileChangeCallbackImpl implements FileChangeCallback {

        @Override
        public void onFileCreated(Path filePath) {
            logger.debug("File created: {}", filePath);

            MappingConfiguration config = findConfigurationByPath(filePath);
            if (config == null) {
                logger.warn("No configuration found for file: {}", filePath);
                return;
            }

            // 提取主键
            String primaryKey = extractPrimaryKeyFromPath(filePath, config);
            if (primaryKey == null) {
                logger.warn("Could not extract primary key from path: {}", filePath);
                return;
            }

            try {
                // 使用 SyncEngine 的带防循环检查的同步方法
                boolean synced = syncEngine.syncFileToDbWithCheck(filePath, config, primaryKey);
                if (synced) {
                    syncLogger.logFileToDatabase(
                            config.getTableName(),
                            filePath.toString(),
                            true,
                            "File created and synced to database");
                    updateLastSyncTime(config);
                }
            } catch (SyncException e) {
                logger.error("Failed to sync created file to database: {}", filePath, e);
                syncLogger.logFileToDatabase(
                        config.getTableName(),
                        filePath.toString(),
                        false,
                        "Failed: " + e.getMessage());
            }
        }

        @Override
        public void onFileModified(Path filePath) {
            logger.debug("File modified: {}", filePath);

            MappingConfiguration config = findConfigurationByPath(filePath);
            if (config == null) {
                logger.warn("No configuration found for file: {}", filePath);
                return;
            }

            // 提取主键
            String primaryKey = extractPrimaryKeyFromPath(filePath, config);
            if (primaryKey == null) {
                logger.warn("Could not extract primary key from path: {}", filePath);
                return;
            }

            try {
                // 使用 SyncEngine 的带防循环检查的同步方法
                boolean synced = syncEngine.syncFileToDbWithCheck(filePath, config, primaryKey);
                if (synced) {
                    syncLogger.logFileToDatabase(
                            config.getTableName(),
                            filePath.toString(),
                            true,
                            "File modified and synced to database");
                    updateLastSyncTime(config);
                }
            } catch (SyncException e) {
                logger.error("Failed to sync modified file to database: {}", filePath, e);
                syncLogger.logFileToDatabase(
                        config.getTableName(),
                        filePath.toString(),
                        false,
                        "Failed: " + e.getMessage());
            }
        }

        /** 从文件路径提取主键 */
        private String extractPrimaryKeyFromPath(Path filePath, MappingConfiguration config) {
            switch (config.getFileStructureMode()) {
                case SINGLE_JSON:
                    // SINGLE_JSON 模式: 主键.json
                    String fileName = filePath.getFileName().toString();
                    if (fileName.endsWith(".json")) {
                        return fileName.substring(0, fileName.length() - 5);
                    }
                    return null;

                case FIELD_FILES:
                    // FIELD_FILES 模式: 表名/主键/字段名
                    // parent 是主键目录
                    Path parent = filePath.getParent();
                    if (parent != null) {
                        return parent.getFileName().toString();
                    }
                    return null;

                default:
                    return null;
            }
        }

        @Override
        public void onFileDeleted(Path filePath) {
            logger.debug("File deleted: {}", filePath);

            MappingConfiguration config = findConfigurationByPath(filePath);
            if (config == null) {
                logger.warn("No configuration found for file: {}", filePath);
                return;
            }

            // 提取主键
            String primaryKey = extractPrimaryKeyFromPath(filePath, config);
            if (primaryKey == null) {
                logger.warn("Could not extract primary key from path: {}", filePath);
                return;
            }

            try {
                // 使用 SyncEngine 的带防循环检查的删除方法
                boolean deleted =
                        syncEngine.handleFileDeletionWithCheck(filePath, config, primaryKey);
                if (deleted) {
                    syncLogger.logFileDeletion(
                            config.getTableName(),
                            filePath.toString(),
                            true,
                            "File deleted and database record removed");
                    updateLastSyncTime(config);
                }
            } catch (SyncException e) {
                logger.error("Failed to handle file deletion: {}", filePath, e);
                syncLogger.logFileDeletion(
                        config.getTableName(),
                        filePath.toString(),
                        false,
                        "Failed: " + e.getMessage());
            }
        }
    }

    /** 数据库变更回调实现 */
    private class DatabaseChangeCallbackImpl implements ChangeCallback {

        private final MappingConfiguration config;

        public DatabaseChangeCallbackImpl(MappingConfiguration config) {
            this.config = config;
        }

        @Override
        public void onInsert(String tableName, Object primaryKey) {
            logger.debug(
                    "Database insert detected: table={}, primaryKey={}", tableName, primaryKey);

            try {
                // 使用 SyncEngine 的带防循环检查的同步方法
                boolean synced = syncEngine.syncDbToFileWithCheck(tableName, primaryKey, config);
                if (synced) {
                    syncLogger.logDatabaseToFile(
                            tableName,
                            primaryKey.toString(),
                            true,
                            "Database insert synced to file");
                    updateLastSyncTime(config);
                }
            } catch (SyncException e) {
                logger.error(
                        "Failed to sync database insert to file: table={}, primaryKey={}",
                        tableName,
                        primaryKey,
                        e);
                syncLogger.logDatabaseToFile(
                        tableName, primaryKey.toString(), false, "Failed: " + e.getMessage());
            }
        }

        @Override
        public void onUpdate(String tableName, Object primaryKey) {
            logger.debug(
                    "Database update detected: table={}, primaryKey={}", tableName, primaryKey);

            try {
                // 使用 SyncEngine 的带防循环检查的同步方法
                boolean synced = syncEngine.syncDbToFileWithCheck(tableName, primaryKey, config);
                if (synced) {
                    syncLogger.logDatabaseToFile(
                            tableName,
                            primaryKey.toString(),
                            true,
                            "Database update synced to file");
                    updateLastSyncTime(config);
                }
            } catch (SyncException e) {
                logger.error(
                        "Failed to sync database update to file: table={}, primaryKey={}",
                        tableName,
                        primaryKey,
                        e);
                syncLogger.logDatabaseToFile(
                        tableName, primaryKey.toString(), false, "Failed: " + e.getMessage());
            }
        }

        @Override
        public void onDelete(String tableName, Object primaryKey) {
            logger.debug(
                    "Database delete detected: table={}, primaryKey={}", tableName, primaryKey);

            try {
                // 使用 SyncEngine 的带防循环检查的删除方法
                boolean deleted =
                        syncEngine.handleDbDeletionWithCheck(tableName, primaryKey, config);
                if (deleted) {
                    syncLogger.logOperation(
                            "DB_DELETE",
                            tableName,
                            true,
                            "Database record deleted and file removed");
                    updateLastSyncTime(config);
                }
            } catch (SyncException e) {
                logger.error(
                        "Failed to handle database deletion: table={}, primaryKey={}",
                        tableName,
                        primaryKey,
                        e);
                syncLogger.logOperation("DB_DELETE", tableName, false, "Failed: " + e.getMessage());
            }
        }
    }
}
