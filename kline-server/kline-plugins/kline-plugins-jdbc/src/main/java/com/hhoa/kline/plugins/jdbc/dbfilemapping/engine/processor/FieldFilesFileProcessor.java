package com.hhoa.kline.plugins.jdbc.dbfilemapping.engine.processor;

import static java.nio.file.Files.isDirectory;

import com.hhoa.kline.plugins.jdbc.dbfilemapping.engine.ErrorHandler;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.engine.RetryPolicy;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.engine.SyncException;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.enums.FieldValueFormat;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.formatter.FieldValueFormatter;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.formatter.FieldValueFormatters;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.helper.FileSystemHelper;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.helper.MetadataManager;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.manager.TableMetadataCache;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.model.MappingConfiguration;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.serializer.JsonSerializer;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.serializer.SerializationException;
import com.hhoa.kline.plugins.jdbc.service.JdbcService;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

class FieldFilesFileProcessor extends AbstractFileProcessor {

    FieldFilesFileProcessor(
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
        logger.debug("Syncing field file to database: {}", filePath);

        Path recordDir = filePath.getParent();
        if (recordDir == null) {
            throw new SyncException("Invalid file path structure: " + filePath);
        }

        String primaryKeyValueStr = fileSystemHelper.extractPrimaryKeyFromDirectory(recordDir);
        Object primaryKeyValue = convertPrimaryKey(config, primaryKeyValueStr);
        String fieldName = filePath.getFileName().toString();

        try {
            retryPolicy.executeVoid(
                    () -> {
                        String fieldValue = fileSystemHelper.readFile(filePath);

                        boolean recordExists = checkRecordExists(config, primaryKeyValue);

                        if (recordExists) {
                            updateSingleField(config, primaryKeyValue, fieldName, fieldValue);
                            logger.info(
                                    "Updated field '{}' in table {} for primary key: {}",
                                    fieldName,
                                    config.getTableName(),
                                    primaryKeyValue);
                        } else {
                            Map<String, String> allFields =
                                    fileSystemHelper.readRecordFields(
                                            config.getTargetDirectory(),
                                            config.getSchemaName(),
                                            config.getTableName(),
                                            primaryKeyValue);

                            Map<String, Object> record = new java.util.HashMap<>(allFields);
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
                            recordDir);
                } catch (Exception e) {
                    logger.warn("Failed to update metadata for record: {}", primaryKeyValue, e);
                }
            }

        } catch (Exception e) {
            ErrorHandler.ErrorHandlingResult result =
                    errorHandler.handleSyncError(config.getTableName(), filePath, e);

            if (result.shouldFailFast()) {
                throw new SyncException(
                        "Fatal error syncing field file to database: " + filePath, e);
            } else if (result.shouldSkip()) {
                logger.warn("Skipping field file sync due to error, file preserved: {}", filePath);
                throw new SyncException("Skipped field file sync: " + filePath, e);
            }

            throw new SyncException("Failed to sync field file to database: " + filePath, e);
        }
    }

    @Override
    public void handleFileDeletion(Path filePath, MappingConfiguration config)
            throws SyncException {
        logger.debug("Handling file deletion: {}", filePath);

        if (isDirectory(filePath)) {
            handleRecordDirectoryDeletion(filePath, config);
        } else {
            handleFieldFileDeletion(filePath, config);
        }
    }

    private void handleRecordDirectoryDeletion(Path recordDir, MappingConfiguration config)
            throws SyncException {
        String primaryKeyValueStr = fileSystemHelper.extractPrimaryKeyFromDirectory(recordDir);
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
                    logger.warn("Failed to remove metadata for record: {}", primaryKeyValue, e);
                }
            }

        } catch (Exception e) {
            ErrorHandler.ErrorHandlingResult result =
                    errorHandler.handleSyncError(config.getTableName(), recordDir, e);

            if (result.shouldFailFast()) {
                throw new SyncException(
                        "Fatal error handling record directory deletion: " + recordDir, e);
            }

            throw new SyncException("Failed to handle record directory deletion: " + recordDir, e);
        }
    }

    private void handleFieldFileDeletion(Path fieldFile, MappingConfiguration config)
            throws SyncException {
        Path recordDir = fieldFile.getParent();
        if (recordDir == null) {
            throw new SyncException("Invalid field file path structure: " + fieldFile);
        }

        String primaryKeyValueStr = fileSystemHelper.extractPrimaryKeyFromDirectory(recordDir);
        Object primaryKeyValue = convertPrimaryKey(config, primaryKeyValueStr);
        String fieldName = fieldFile.getFileName().toString();

        try {
            retryPolicy.executeVoid(
                    () -> {
                        updateSingleField(config, primaryKeyValue, fieldName, null);
                        logger.info(
                                "Set field '{}' to NULL in table {} for primary key: {}",
                                fieldName,
                                config.getTableName(),
                                primaryKeyValue);
                    });

        } catch (Exception e) {
            ErrorHandler.ErrorHandlingResult result =
                    errorHandler.handleSyncError(config.getTableName(), fieldFile, e);

            if (result.shouldFailFast()) {
                throw new SyncException(
                        "Fatal error handling field file deletion: " + fieldFile, e);
            } else if (result.shouldSkip()) {
                logger.warn("Skipping field file deletion handling due to error: {}", fieldFile);
                throw new SyncException("Skipped field file deletion handling", e);
            }

            throw new SyncException("Failed to handle field file deletion: " + fieldFile, e);
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

        Map<String, Object> processedRecord = new LinkedHashMap<>();
        Map<String, String> dbSyncBack = new LinkedHashMap<>();
        Map<String, FieldValueFormat> formats = config.getFieldValueFormats();
        Map<String, Boolean> formatEnabled = config.getFieldValueFormatEnabled();

        for (Map.Entry<String, Object> entry : filteredRecord.entrySet()) {
            String fieldName = entry.getKey();
            Object rawValue = entry.getValue();
            String rawStr = rawValue == null ? "" : rawValue.toString();

            String valueToWrite = rawStr;
            if (formats != null
                    && formats.containsKey(fieldName)
                    && (formatEnabled == null
                            || Boolean.TRUE.equals(formatEnabled.get(fieldName)))) {
                FieldValueFormatter formatter = FieldValueFormatters.get(formats.get(fieldName));
                try {
                    valueToWrite = formatter.format(rawStr);
                    if (!rawStr.equals(valueToWrite)) {
                        dbSyncBack.put(fieldName, valueToWrite);
                    }
                } catch (Exception e) {
                    logger.warn("字段 {} 格式化失败，使用原始值: {}", fieldName, e.getMessage());
                }
            }

            processedRecord.put(fieldName, valueToWrite);
        }

        if (fileSystemHelper.recordDirectoryExists(
                config.getTargetDirectory(),
                config.getSchemaName(),
                config.getTableName(),
                primaryKeyValue)) {
            try {
                Map<String, String> existingFields =
                        fileSystemHelper.readRecordFields(
                                config.getTargetDirectory(),
                                config.getSchemaName(),
                                config.getTableName(),
                                primaryKeyValue);

                boolean unchanged = true;
                for (Map.Entry<String, Object> entry : processedRecord.entrySet()) {
                    String fieldName = entry.getKey();
                    String newValue = entry.getValue() == null ? "" : entry.getValue().toString();
                    String existingValue = existingFields.get(fieldName);

                    if (!newValue.equals(existingValue)) {
                        unchanged = false;
                        break;
                    }
                }

                if (unchanged && existingFields.size() == processedRecord.size()) {
                    logger.debug(
                            "Record fields unchanged, skipping write for primary key: {}",
                            primaryKeyValue);
                    return;
                }
            } catch (IOException e) {
                logger.warn(
                        "Failed to read existing record fields for comparison: {}",
                        primaryKeyValue);
            }
        }

        fileSystemHelper.writeRecordFields(
                config.getTargetDirectory(),
                config.getSchemaName(),
                config.getTableName(),
                primaryKeyValue,
                processedRecord);

        for (Map.Entry<String, String> entry : dbSyncBack.entrySet()) {
            try {
                updateSingleField(config, primaryKeyValue, entry.getKey(), entry.getValue());
                logger.debug("格式化后同步回 DB：字段 {} 主键 {}", entry.getKey(), primaryKeyValue);
            } catch (Exception e) {
                logger.warn(
                        "格式化值同步回 DB 失败：字段 {} 主键 {}: {}",
                        entry.getKey(),
                        primaryKeyValue,
                        e.getMessage());
            }
        }

        if (metadataManager != null) {
            try {
                Path recordDir =
                        fileSystemHelper.getRecordDirectoryPath(
                                config.getTargetDirectory(),
                                config.getSchemaName(),
                                config.getTableName(),
                                primaryKeyValue);
                metadataManager.updateFileMetadata(
                        config.getSchemaName(),
                        config.getTableName(),
                        primaryKeyValue.toString(),
                        recordDir);
            } catch (Exception e) {
                logger.warn("Failed to update metadata for record: {}", primaryKeyValue, e);
            }
        }

        logger.debug(
                "Created/updated {} field files for record: {}",
                processedRecord.size(),
                primaryKeyValue);
    }

    @Override
    public void handleDatabaseDeletion(
            String tableName, Object primaryKey, MappingConfiguration config) throws SyncException {
        try {
            fileSystemHelper.deleteRecordDirectory(
                    config.getTargetDirectory(),
                    config.getSchemaName(),
                    config.getTableName(),
                    primaryKey);
            logger.info("Deleted record directory for primaryKey: {}", primaryKey);

            metadataManager.removeFileMetadata(
                    config.getSchemaName(), config.getTableName(), primaryKey.toString());

        } catch (Exception e) {
            throw new SyncException(
                    "Failed to handle database deletion for primaryKey: " + primaryKey, e);
        }
    }

    @Override
    public int getSyncIncrementCount(MappingConfiguration config, Object primaryKey) {
        try {
            Map<String, Object> record = fetchRecord(config, primaryKey);
            if (record != null && !record.isEmpty()) {
                int filteredSize = applyFieldFilter(record, config).size();
                return filteredSize > 0 ? filteredSize : 1;
            }
        } catch (Exception e) {
            logger.warn("Failed to fetch record for field count, using default increment", e);
        }
        return 1;
    }
}
