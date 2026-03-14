package com.hhoa.kline.plugins.jdbc.dbfilemapping.engine;

import com.hhoa.kline.plugins.jdbc.dbfilemapping.engine.processor.FileProcessorFactory;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.helper.FileSystemHelper;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.helper.MetadataManager;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.helper.SyncContext;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.manager.CheckpointManager;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.manager.TableMetadataCache;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.model.MappingConfiguration;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.model.SyncCheckpoint;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.serializer.JsonSerializer;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.serializer.SerializationException;
import com.hhoa.kline.plugins.jdbc.service.JdbcService;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 默认同步引擎实现 Default implementation of SyncEngine with batch processing and checkpoint support
 * Enhanced with error handling and retry mechanisms
 *
 * <p>Requirements: 5.4, 10.1, 10.2, 10.5
 */
public class DefaultSyncEngine implements SyncEngine {

    private static final Logger logger = LoggerFactory.getLogger(DefaultSyncEngine.class);
    private static final int CHECKPOINT_INTERVAL = 10; // 每处理10批创建一个检查点

    private final JdbcService jdbcService;
    private final JsonSerializer jsonSerializer;
    private final FileSystemHelper fileSystemHelper;
    private final CheckpointManager checkpointManager;
    private final ErrorHandler errorHandler;
    private final RetryPolicy retryPolicy;
    private final MetadataManager metadataManager;
    private final TableMetadataCache tableMetadataCache;
    private final FileProcessorFactory fileProcessorFactory;

    public DefaultSyncEngine(
            JdbcService jdbcService,
            JsonSerializer jsonSerializer,
            FileSystemHelper fileSystemHelper,
            CheckpointManager checkpointManager,
            ErrorHandler errorHandler,
            String baseDirectory,
            TableMetadataCache tableMetadataCache) {
        if (jdbcService == null) {
            throw new IllegalArgumentException("JdbcService cannot be null");
        }
        if (jsonSerializer == null) {
            throw new IllegalArgumentException("JsonSerializer cannot be null");
        }
        if (fileSystemHelper == null) {
            throw new IllegalArgumentException("FileSystemHelper cannot be null");
        }
        if (checkpointManager == null) {
            throw new IllegalArgumentException("CheckpointManager cannot be null");
        }
        if (errorHandler == null) {
            throw new IllegalArgumentException("ErrorHandler cannot be null");
        }
        if (tableMetadataCache == null) {
            throw new IllegalArgumentException("TableMetadataCache cannot be null");
        }

        this.jdbcService = jdbcService;
        this.jsonSerializer = jsonSerializer;
        this.fileSystemHelper = fileSystemHelper;
        this.checkpointManager = checkpointManager;
        this.errorHandler = errorHandler;
        this.retryPolicy = new RetryPolicy();
        this.metadataManager = new MetadataManager(baseDirectory);
        this.tableMetadataCache = tableMetadataCache;
        this.fileProcessorFactory =
                new FileProcessorFactory(
                        jdbcService,
                        jsonSerializer,
                        fileSystemHelper,
                        tableMetadataCache,
                        metadataManager,
                        this.retryPolicy,
                        errorHandler);
    }

    @Override
    public void initializeSync(MappingConfiguration config) throws SyncException {
        initializeSync(config, null);
    }

    @Override
    public void initializeSync(MappingConfiguration config, SyncProgressListener progressListener)
            throws SyncException {
        if (config == null) {
            throw new SyncException("Configuration cannot be null");
        }

        logger.info("Initializing sync for table: {}", config.getQualifiedTableName());

        try {
            // 创建目录结构
            fileSystemHelper.createTableDirectory(
                    config.getTargetDirectory(), config.getSchemaName(), config.getTableName());

            // 检查是否存在未完成的检查点
            SyncCheckpoint existingCheckpoint =
                    checkpointManager.loadCheckpoint(config.getSchemaName(), config.getTableName());

            if (existingCheckpoint != null && !existingCheckpoint.isCompleted()) {
                logger.info(
                        "Found incomplete checkpoint for table {}, resuming from record {}",
                        config.getQualifiedTableName(),
                        existingCheckpoint.getProcessedRecords());
                resumeSync(config, existingCheckpoint, progressListener);
                return;
            }

            // 获取总记录数
            long totalRecords = getTotalRecordCount(config);

            // 通知开始同步
            if (progressListener != null) {
                progressListener.onSyncStarted(config.getQualifiedTableName(), totalRecords);
            }

            // 创建新检查点
            SyncCheckpoint checkpoint =
                    new SyncCheckpoint(config.getSchemaName(), config.getTableName());
            checkpoint.setTotalRecords(totalRecords);

            // 批量处理记录
            processBatchesWithCheckpoint(config, checkpoint, progressListener, 0);

            // 标记完成并删除检查点
            checkpoint.setCompleted(true);
            checkpointManager.deleteCheckpoint(config.getSchemaName(), config.getTableName());

            removeOrphanedFiles(config);

            // 通知完成
            if (progressListener != null) {
                progressListener.onSyncCompleted(
                        config.getQualifiedTableName(),
                        checkpoint.getProcessedRecords(),
                        checkpoint.getFailedRecords());
            }

            logger.info(
                    "Initialization complete for table {}: {} records processed, {} failed",
                    config.getTableName(),
                    checkpoint.getProcessedRecords(),
                    checkpoint.getFailedRecords());

        } catch (Exception e) {
            if (progressListener != null) {
                progressListener.onError(config.getQualifiedTableName(), e);
            }
            throw new SyncException(
                    "Failed to initialize sync for table " + config.getTableName(), e);
        }
    }

    @Override
    public void resumeSync(
            MappingConfiguration config,
            SyncCheckpoint checkpoint,
            SyncProgressListener progressListener)
            throws SyncException {
        if (config == null || checkpoint == null) {
            throw new SyncException("Configuration and checkpoint cannot be null");
        }

        logger.info(
                "Resuming sync for table {} from checkpoint: {} records already processed",
                config.getQualifiedTableName(),
                checkpoint.getProcessedRecords());

        try {
            // 从检查点位置继续处理
            long startOffset = checkpoint.getProcessedRecords();

            // 通知开始同步（恢复）
            if (progressListener != null) {
                progressListener.onSyncStarted(
                        config.getQualifiedTableName(), checkpoint.getTotalRecords());
            }

            // 继续批量处理
            processBatchesWithCheckpoint(config, checkpoint, progressListener, startOffset);

            // 标记完成并删除检查点
            checkpoint.setCompleted(true);
            checkpointManager.deleteCheckpoint(config.getSchemaName(), config.getTableName());

            removeOrphanedFiles(config);

            // 通知完成
            if (progressListener != null) {
                progressListener.onSyncCompleted(
                        config.getQualifiedTableName(),
                        checkpoint.getProcessedRecords(),
                        checkpoint.getFailedRecords());
            }

            logger.info(
                    "Resume complete for table {}: {} total records processed, {} failed",
                    config.getTableName(),
                    checkpoint.getProcessedRecords(),
                    checkpoint.getFailedRecords());

        } catch (Exception e) {
            if (progressListener != null) {
                progressListener.onError(config.getQualifiedTableName(), e);
            }
            throw new SyncException("Failed to resume sync for table " + config.getTableName(), e);
        }
    }

    /** 批量处理记录并创建检查点 */
    private void processBatchesWithCheckpoint(
            MappingConfiguration config,
            SyncCheckpoint checkpoint,
            SyncProgressListener progressListener,
            long startOffset)
            throws IOException {
        int batchSize = config.getMaxBatchSize();
        long offset = startOffset;
        int batchCount = 0;
        Object lastPrimaryKey = checkpoint.getLastProcessedPrimaryKey();

        while (true) {
            List<Map<String, Object>> records = fetchRecordsBatch(config, (int) offset, batchSize);

            if (records.isEmpty()) {
                break;
            }

            int batchProcessed = 0;
            int batchFailed = 0;

            // 处理每条记录
            for (Map<String, Object> record : records) {
                try {
                    // 使用重试策略处理可能的临时错误
                    retryPolicy.executeVoid(() -> createFileFromRecord(record, config));
                    batchProcessed++;

                    // 记录最后处理的主键
                    lastPrimaryKey = record.get(config.getPrimaryKeyColumn());

                } catch (Exception e) {
                    batchFailed++;

                    // 使用ErrorHandler处理错误
                    ErrorHandler.ErrorHandlingResult result =
                            errorHandler.handleSyncError(config.getTableName(), null, e);

                    if (result.shouldFailFast()) {
                        // 快速失败：配置错误等不可恢复的错误
                        logger.error("Fatal error during batch processing, aborting", e);
                        throw new IOException("Fatal error during sync", e);
                    }

                    // 跳过并记录：继续处理其他记录（符合Property 10: Serialization error isolation）
                    logger.warn(
                            "Skipping record due to error (will continue with others): {}",
                            e.getMessage());
                }
            }

            // 更新检查点
            long totalProcessed = checkpoint.getProcessedRecords() + batchProcessed;
            long totalFailed = checkpoint.getFailedRecords() + batchFailed;
            checkpoint.update(lastPrimaryKey, totalProcessed, totalFailed);

            // 通知批次处理完成
            if (progressListener != null) {
                progressListener.onBatchProcessed(
                        config.getQualifiedTableName(),
                        totalProcessed,
                        checkpoint.getTotalRecords(),
                        batchProcessed);
            }

            // 定期保存检查点
            batchCount++;
            if (batchCount % CHECKPOINT_INTERVAL == 0) {
                checkpointManager.saveCheckpoint(checkpoint);
                if (progressListener != null) {
                    progressListener.onCheckpointCreated(checkpoint);
                }
                logger.debug(
                        "Checkpoint saved for table {}: {} records processed",
                        config.getQualifiedTableName(),
                        totalProcessed);
            }

            offset += batchSize;

            logger.debug(
                    "Processed batch {} for table {}: {} records in batch, {} total processed",
                    batchCount,
                    config.getTableName(),
                    batchProcessed,
                    totalProcessed);
        }

        // 保存最终检查点
        checkpointManager.saveCheckpoint(checkpoint);
        if (progressListener != null) {
            progressListener.onCheckpointCreated(checkpoint);
        }
    }

    /** 获取表的总记录数 */
    private long getTotalRecordCount(MappingConfiguration config) {
        return jdbcService.countRecords(config.getQualifiedTableName());
    }

    @Override
    public void syncFileToDatabase(Path filePath, MappingConfiguration config)
            throws SyncException {
        if (filePath == null || config == null) {
            throw new SyncException("File path and configuration cannot be null");
        }

        fileProcessorFactory
                .getProcessor(config.getFileStructureMode())
                .syncFileToDatabase(filePath, config);
    }

    @Override
    public void syncDatabaseToFile(String tableName, Object primaryKey, MappingConfiguration config)
            throws SyncException {
        if (tableName == null || primaryKey == null || config == null) {
            throw new SyncException("Table name, primary key, and configuration cannot be null");
        }

        logger.debug("Syncing database to file: table={}, primaryKey={}", tableName, primaryKey);

        try {
            // 转换主键值为正确的数据库类型
            Object convertedPrimaryKey = tableMetadataCache.convertPrimaryKey(config, primaryKey);

            // 使用重试策略执行同步操作
            final Object finalPrimaryKey = convertedPrimaryKey;
            retryPolicy.executeVoid(
                    () -> {
                        // 查询数据库记录
                        Map<String, Object> record = fetchRecord(config, finalPrimaryKey);

                        if (record == null || record.isEmpty()) {
                            logger.warn(
                                    "Record not found in table {} with primary key: {}",
                                    tableName,
                                    finalPrimaryKey);
                            return;
                        }

                        // 创建或更新文件
                        createFileFromRecord(record, config);

                        logger.info(
                                "Synced database record to file: table={}, primaryKey={}",
                                tableName,
                                finalPrimaryKey);
                    });

        } catch (Exception e) {
            // 使用ErrorHandler处理错误
            ErrorHandler.ErrorHandlingResult result =
                    errorHandler.handleSyncError(tableName, null, e);

            if (result.shouldFailFast()) {
                throw new SyncException(
                        "Fatal error syncing database to file for table " + tableName, e);
            } else if (result.shouldSkip()) {
                logger.warn(
                        "Skipping database-to-file sync due to error: table={}, primaryKey={}",
                        tableName,
                        primaryKey);
                throw new SyncException("Skipped database-to-file sync", e);
            }

            throw new SyncException("Failed to sync database to file for table " + tableName, e);
        }
    }

    @Override
    public void handleFileDeletion(Path filePath, MappingConfiguration config)
            throws SyncException {
        if (filePath == null || config == null) {
            throw new SyncException("File path and configuration cannot be null");
        }

        fileProcessorFactory
                .getProcessor(config.getFileStructureMode())
                .handleFileDeletion(filePath, config);
    }

    // ========== Private Helper Methods ==========

    private void removeOrphanedFiles(MappingConfiguration config) {
        Set<String> dbPrimaryKeys = fetchAllPrimaryKeys(config);
        Set<String> diskPrimaryKeys =
                fileSystemHelper.listRecordPrimaryKeysOnDisk(
                        config.getTargetDirectory(),
                        config.getSchemaName(),
                        config.getTableName(),
                        config.getFileStructureMode());

        for (String pk : diskPrimaryKeys) {
            if (!dbPrimaryKeys.contains(pk)) {
                try {
                    handleDatabaseDeletion(config.getTableName(), pk, config);
                    logger.info(
                            "Removed orphaned file for deleted record: table={}, primaryKey={}",
                            config.getTableName(),
                            pk);
                } catch (Exception e) {
                    logger.warn(
                            "Failed to remove orphaned file: table={}, primaryKey={}",
                            config.getTableName(),
                            pk,
                            e);
                }
            }
        }
    }

    private Set<String> fetchAllPrimaryKeys(MappingConfiguration config) {
        String sql =
                "SELECT "
                        + config.getPrimaryKeyColumn()
                        + " FROM "
                        + config.getQualifiedTableName();
        List<Map<String, Object>> rows = jdbcService.queryForMapList(sql);
        Set<String> primaryKeys = new HashSet<>();
        for (Map<String, Object> row : rows) {
            Object pk = row.get(config.getPrimaryKeyColumn());
            if (pk != null) {
                primaryKeys.add(pk.toString());
            }
        }
        return primaryKeys;
    }

    /** 批量获取数据库记录 */
    private List<Map<String, Object>> fetchRecordsBatch(
            MappingConfiguration config, int offset, int limit) {
        return jdbcService.queryWithPagination(config.getQualifiedTableName(), offset, limit);
    }

    /** 获取单条数据库记录 */
    private Map<String, Object> fetchRecord(MappingConfiguration config, Object primaryKey) {
        // 转换主键值为正确的数据库类型
        Object convertedPrimaryKey = tableMetadataCache.convertPrimaryKey(config, primaryKey);

        return jdbcService.queryByPrimaryKey(
                config.getQualifiedTableName(), config.getPrimaryKeyColumn(), convertedPrimaryKey);
    }

    private void createFileFromRecord(Map<String, Object> record, MappingConfiguration config)
            throws SerializationException, IOException {
        fileProcessorFactory
                .getProcessor(config.getFileStructureMode())
                .createFileFromRecord(record, config);
    }

    /** 带防循环检查的文件到数据库同步 */
    @Override
    public boolean syncFileToDbWithCheck(
            Path filePath, MappingConfiguration config, Object primaryKey) throws SyncException {
        synchronized (SyncEngine.class) {
            // 检查并减少反向计数器
            if (SyncContext.checkAndDecrementOppositeSync(
                    config.getSchemaName(),
                    config.getTableName(),
                    primaryKey,
                    SyncContext.SyncDirection.FILE_TO_DB)) {
                return false;
            }

            // 增加当前方向计数器
            SyncContext.incrementSync(
                    config.getSchemaName(),
                    config.getTableName(),
                    primaryKey,
                    SyncContext.SyncDirection.FILE_TO_DB);

            // 执行同步
            syncFileToDatabase(filePath, config);
            return true;
        }
    }

    /** 带防循环检查的数据库到文件同步 */
    @Override
    public boolean syncDbToFileWithCheck(
            String tableName, Object primaryKey, MappingConfiguration config) throws SyncException {
        synchronized (SyncEngine.class) {
            // 检查并减少反向计数器
            if (SyncContext.checkAndDecrementOppositeSync(
                    config.getSchemaName(),
                    config.getTableName(),
                    primaryKey,
                    SyncContext.SyncDirection.DB_TO_FILE)) {
                logger.debug(
                        "Skipping db-to-file sync (triggered by opposite direction): table={}, primaryKey={}",
                        tableName,
                        primaryKey);
                return false;
            }

            int incrementCount =
                    fileProcessorFactory
                            .getProcessor(config.getFileStructureMode())
                            .getSyncIncrementCount(config, primaryKey);
            if (incrementCount > 1) {
                SyncContext.incrementSyncBy(
                        config.getSchemaName(),
                        config.getTableName(),
                        primaryKey,
                        SyncContext.SyncDirection.DB_TO_FILE,
                        incrementCount);
            } else {
                SyncContext.incrementSync(
                        config.getSchemaName(),
                        config.getTableName(),
                        primaryKey,
                        SyncContext.SyncDirection.DB_TO_FILE);
            }

            // 执行同步
            syncDatabaseToFile(tableName, primaryKey, config);
            return true;
        }
    }

    /** 处理数据库记录删除 */
    @Override
    public void handleDatabaseDeletion(
            String tableName, Object primaryKey, MappingConfiguration config) throws SyncException {
        fileProcessorFactory
                .getProcessor(config.getFileStructureMode())
                .handleDatabaseDeletion(tableName, primaryKey, config);
    }

    /** 带防循环检查的文件删除处理 */
    @Override
    public boolean handleFileDeletionWithCheck(
            Path filePath, MappingConfiguration config, Object primaryKey) throws SyncException {
        // 检查并减少反向计数器
        if (SyncContext.checkAndDecrementOppositeSync(
                config.getSchemaName(),
                config.getTableName(),
                primaryKey,
                SyncContext.SyncDirection.FILE_TO_DB)) {
            logger.debug(
                    "Skipping file deletion handling (triggered by opposite direction): {}",
                    filePath);
            return false;
        }

        // 增加当前方向计数器
        SyncContext.incrementSync(
                config.getSchemaName(),
                config.getTableName(),
                primaryKey,
                SyncContext.SyncDirection.FILE_TO_DB);

        // 执行删除
        handleFileDeletion(filePath, config);
        return true;
    }

    /** 带防循环检查的数据库删除处理 */
    @Override
    public boolean handleDbDeletionWithCheck(
            String tableName, Object primaryKey, MappingConfiguration config) throws SyncException {
        // 检查并减少反向计数器
        if (SyncContext.checkAndDecrementOppositeSync(
                config.getSchemaName(),
                config.getTableName(),
                primaryKey,
                SyncContext.SyncDirection.DB_TO_FILE)) {
            logger.debug(
                    "Skipping db deletion handling (triggered by opposite direction): table={}, primaryKey={}",
                    tableName,
                    primaryKey);
            return false;
        }

        // 增加当前方向计数器
        SyncContext.incrementSync(
                config.getSchemaName(),
                config.getTableName(),
                primaryKey,
                SyncContext.SyncDirection.DB_TO_FILE);

        // 执行删除
        handleDatabaseDeletion(tableName, primaryKey, config);
        return true;
    }
}
