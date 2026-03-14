package com.hhoa.kline.plugins.jdbc.dbfilemapping.engine.processor;

import com.hhoa.kline.plugins.jdbc.dbfilemapping.engine.ErrorHandler;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.engine.RetryPolicy;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.helper.FileSystemHelper;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.helper.MetadataManager;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.helper.RecordFieldFilter;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.manager.TableMetadataCache;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.model.MappingConfiguration;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.model.ValidationResult;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.serializer.JsonSerializer;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.serializer.SerializationException;
import com.hhoa.kline.plugins.jdbc.service.JdbcService;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class AbstractFileProcessor implements FileProcessor {

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected final JdbcService jdbcService;
    protected final JsonSerializer jsonSerializer;
    protected final FileSystemHelper fileSystemHelper;
    protected final TableMetadataCache tableMetadataCache;
    protected final MetadataManager metadataManager;
    protected final RetryPolicy retryPolicy;
    protected final ErrorHandler errorHandler;

    protected AbstractFileProcessor(
            JdbcService jdbcService,
            JsonSerializer jsonSerializer,
            FileSystemHelper fileSystemHelper,
            TableMetadataCache tableMetadataCache,
            MetadataManager metadataManager,
            RetryPolicy retryPolicy,
            ErrorHandler errorHandler) {
        this.jdbcService = jdbcService;
        this.jsonSerializer = jsonSerializer;
        this.fileSystemHelper = fileSystemHelper;
        this.tableMetadataCache = tableMetadataCache;
        this.metadataManager = metadataManager;
        this.retryPolicy = retryPolicy;
        this.errorHandler = errorHandler;
    }

    protected Object convertPrimaryKey(MappingConfiguration config, String primaryKeyValue) {
        return tableMetadataCache.convertPrimaryKey(config, primaryKeyValue);
    }

    protected boolean checkRecordExists(MappingConfiguration config, Object primaryKey) {
        return jdbcService.recordExists(
                config.getQualifiedTableName(), config.getPrimaryKeyColumn(), primaryKey);
    }

    protected void insertRecord(
            MappingConfiguration config, Map<String, Object> record, Object primaryKey) {
        Map<String, Object> convertedRecord = new java.util.HashMap<>();
        for (Map.Entry<String, Object> entry : record.entrySet()) {
            String fieldName = entry.getKey();
            Object fieldValue = entry.getValue();
            Object convertedValue =
                    tableMetadataCache.convertFieldValue(
                            config.getSchemaName(), config.getTableName(), fieldName, fieldValue);
            convertedRecord.put(fieldName, convertedValue);
        }
        String pkColumn = config.getPrimaryKeyColumn();
        if (!convertedRecord.containsKey(pkColumn)) {
            Object convertedPk = tableMetadataCache.convertPrimaryKey(config, primaryKey);
            convertedRecord.put(pkColumn, convertedPk);
        }
        jdbcService.insertRecord(config.getQualifiedTableName(), convertedRecord);
    }

    protected void updateRecord(
            MappingConfiguration config, Map<String, Object> record, Object primaryKey) {
        Map<String, Object> convertedRecord = new java.util.HashMap<>();
        for (Map.Entry<String, Object> entry : record.entrySet()) {
            String fieldName = entry.getKey();
            Object fieldValue = entry.getValue();
            Object convertedValue =
                    tableMetadataCache.convertFieldValue(
                            config.getSchemaName(), config.getTableName(), fieldName, fieldValue);
            convertedRecord.put(fieldName, convertedValue);
        }
        jdbcService.updateRecord(
                config.getQualifiedTableName(),
                convertedRecord,
                config.getPrimaryKeyColumn(),
                primaryKey);
    }

    protected void updateSingleField(
            MappingConfiguration config,
            Object primaryKeyValue,
            String fieldName,
            String fieldValue) {
        Object convertedValue =
                tableMetadataCache.convertFieldValue(
                        config.getSchemaName(), config.getTableName(), fieldName, fieldValue);
        jdbcService.updateField(
                config.getQualifiedTableName(),
                fieldName,
                convertedValue,
                config.getPrimaryKeyColumn(),
                primaryKeyValue);
    }

    protected void deleteRecord(MappingConfiguration config, Object primaryKey) {
        jdbcService.deleteRecord(
                config.getQualifiedTableName(), config.getPrimaryKeyColumn(), primaryKey);
    }

    protected Map<String, Object> applyFieldFilter(
            Map<String, Object> record, MappingConfiguration config) {
        return RecordFieldFilter.applyForSerialization(record, config);
    }

    protected ValidationResult validateFileContent(
            String jsonContent, MappingConfiguration config) {
        ValidationResult result = new ValidationResult();

        if (jsonContent == null || jsonContent.trim().isEmpty()) {
            result.addError("JSON content is empty");
            return result;
        }

        try {
            Map<String, Object> record = jsonSerializer.deserialize(jsonContent, config);

            if (config != null && config.getPrimaryKeyColumn() != null) {
                if (!record.containsKey(config.getPrimaryKeyColumn())) {
                    result.addError("Missing required field: " + config.getPrimaryKeyColumn());
                }
            }

            if (record.isEmpty()) {
                result.addError("Record contains no fields");
            }

        } catch (SerializationException e) {
            result.addError("Invalid JSON format: " + e.getMessage());
        }

        return result;
    }

    protected Map<String, Object> fetchRecord(MappingConfiguration config, Object primaryKey) {
        Object convertedPrimaryKey = tableMetadataCache.convertPrimaryKey(config, primaryKey);
        return jdbcService.queryByPrimaryKey(
                config.getQualifiedTableName(), config.getPrimaryKeyColumn(), convertedPrimaryKey);
    }
}
