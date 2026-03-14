package com.hhoa.kline.plugins.jdbc.dbfilemapping.engine.processor;

import com.hhoa.kline.plugins.jdbc.dbfilemapping.engine.ErrorHandler;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.engine.RetryPolicy;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.engine.SyncException;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.helper.FileSystemHelper;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.helper.MetadataManager;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.manager.TableMetadataCache;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.model.MappingConfiguration;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.model.ValidationResult;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.serializer.JsonSerializer;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.serializer.SerializationException;
import com.hhoa.kline.plugins.jdbc.service.JdbcService;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

class SingleJsonFileProcessor extends AbstractFileProcessor {

    SingleJsonFileProcessor(
            JdbcService jdbcService,
            JsonSerializer jsonSerializer,
            FileSystemHelper fileSystemHelper,
            TableMetadataCache tableMetadataCache,
            MetadataManager metadataManager,
            RetryPolicy retryPolicy,
            ErrorHandler errorHandler) {
        super(
                jdbcService,
                jsonSerializer,
                fileSystemHelper,
                tableMetadataCache,
                metadataManager,
                retryPolicy,
                errorHandler);
    }

    @Override
    public void syncFileToDatabase(Path filePath, MappingConfiguration config)
            throws SyncException {
        logger.debug("Syncing JSON file to database: {}", filePath);

        String primaryKeyValueStr = fileSystemHelper.extractPrimaryKeyFromPath(filePath);
        Object primaryKeyValue = convertPrimaryKey(config, primaryKeyValueStr);

        try {
            retryPolicy.executeVoid(
                    () -> {
                        String jsonContent = fileSystemHelper.readFile(filePath);

                        ValidationResult validation = validateFileContent(jsonContent, config);
                        if (!validation.isValid()) {
                            throw new SyncException(
                                    "Invalid file content: " + validation.getErrors());
                        }

                        Map<String, Object> record =
                                jsonSerializer.deserialize(jsonContent, config);

                        boolean recordExists = checkRecordExists(config, primaryKeyValue);

                        if (recordExists) {
                            updateRecord(config, record, primaryKeyValue);
                            logger.info(
                                    "Updated record in table {} with primary key: {}",
                                    config.getTableName(),
                                    primaryKeyValue);
                        } else {
                            insertRecord(config, record, primaryKeyValue);
                            logger.info(
                                    "Inserted new record in table {} with primary key: {}",
                                    config.getTableName(),
                                    primaryKeyValue);
                        }
                    });

            if (metadataManager != null) {
                try {
                    metadataManager.updateFileMetadata(
                            config.getSchemaName(),
                            config.getTableName(),
                            primaryKeyValue.toString(),
                            filePath);
                } catch (Exception e) {
                    logger.warn("Failed to update metadata for file: {}", filePath, e);
                }
            }

        } catch (Exception e) {
            ErrorHandler.ErrorHandlingResult result =
                    errorHandler.handleSyncError(config.getTableName(), filePath, e);

            if (result.shouldFailFast()) {
                throw new SyncException(
                        "Fatal error syncing JSON file to database: " + filePath, e);
            } else if (result.shouldSkip()) {
                logger.warn("Skipping JSON file sync due to error, file preserved: {}", filePath);
                throw new SyncException("Skipped JSON file sync: " + filePath, e);
            }

            throw new SyncException("Failed to sync JSON file to database: " + filePath, e);
        }
    }

    @Override
    public void handleFileDeletion(Path filePath, MappingConfiguration config)
            throws SyncException {
        logger.debug("Handling JSON file deletion: {}", filePath);

        String primaryKeyValueStr = fileSystemHelper.extractPrimaryKeyFromPath(filePath);
        Object primaryKeyValue = convertPrimaryKey(config, primaryKeyValueStr);

        try {
            retryPolicy.executeVoid(
                    () -> {
                        deleteRecord(config, primaryKeyValue);
                        logger.info(
                                "Deleted record from table {} with primary key: {}",
                                config.getTableName(),
                                primaryKeyValue);
                    });

            if (metadataManager != null) {
                try {
                    metadataManager.removeFileMetadata(
                            config.getSchemaName(),
                            config.getTableName(),
                            primaryKeyValue.toString());
                } catch (Exception e) {
                    logger.warn("Failed to remove metadata for file: {}", filePath, e);
                }
            }

        } catch (Exception e) {
            ErrorHandler.ErrorHandlingResult result =
                    errorHandler.handleSyncError(config.getTableName(), filePath, e);

            if (result.shouldFailFast()) {
                throw new SyncException("Fatal error handling JSON file deletion: " + filePath, e);
            }

            throw new SyncException("Failed to handle JSON file deletion: " + filePath, e);
        }
    }

    @Override
    public void createFileFromRecord(Map<String, Object> record, MappingConfiguration config)
            throws SerializationException, IOException {
        Object primaryKeyValue = record.get(config.getPrimaryKeyColumn());
        if (primaryKeyValue == null) {
            throw new IllegalArgumentException("Primary key value is null for record");
        }

        Map<String, Object> filteredRecord = applyFieldFilter(record, config);

        Path filePath =
                fileSystemHelper.getRecordFilePath(
                        config.getTargetDirectory(),
                        config.getSchemaName(),
                        config.getTableName(),
                        primaryKeyValue);

        if (fileSystemHelper.fileExists(filePath)) {
            try {
                String existingContent = fileSystemHelper.readFile(filePath);
                String newContent = jsonSerializer.serialize(filteredRecord, config);

                if (existingContent.equals(newContent)) {
                    logger.debug("File content unchanged, skipping write: {}", filePath);
                    return;
                }
            } catch (IOException e) {
                logger.warn("Failed to read existing file for comparison: {}", filePath);
            }
        }

        String jsonContent = jsonSerializer.serialize(filteredRecord, config);
        fileSystemHelper.writeFile(filePath, jsonContent);

        if (metadataManager != null) {
            try {
                metadataManager.updateFileMetadata(
                        config.getSchemaName(),
                        config.getTableName(),
                        primaryKeyValue.toString(),
                        filePath);
            } catch (Exception e) {
                logger.warn("Failed to update metadata for file: {}", filePath, e);
            }
        }

        logger.debug("Created/updated JSON file for record: {}", primaryKeyValue);
    }

    @Override
    public void handleDatabaseDeletion(
            String tableName, Object primaryKey, MappingConfiguration config) throws SyncException {
        try {
            Path filePath =
                    fileSystemHelper.getRecordFilePath(
                            config.getTargetDirectory(),
                            config.getSchemaName(),
                            config.getTableName(),
                            primaryKey);
            fileSystemHelper.deleteFile(filePath);
            logger.info("Deleted file: {}", filePath);

            metadataManager.removeFileMetadata(
                    config.getSchemaName(), config.getTableName(), primaryKey.toString());

        } catch (Exception e) {
            throw new SyncException(
                    "Failed to handle database deletion for primaryKey: " + primaryKey, e);
        }
    }

    @Override
    public int getSyncIncrementCount(MappingConfiguration config, Object primaryKey) {
        return 1;
    }
}
