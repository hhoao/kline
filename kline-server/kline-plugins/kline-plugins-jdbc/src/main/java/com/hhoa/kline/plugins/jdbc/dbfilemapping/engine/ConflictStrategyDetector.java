package com.hhoa.kline.plugins.jdbc.dbfilemapping.engine;

import com.hhoa.kline.plugins.jdbc.dbfilemapping.enums.ConflictStrategy;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.model.MappingConfiguration;
import com.hhoa.kline.plugins.jdbc.service.JdbcService;
import com.hhoa.kline.plugins.jdbc.service.TableColumnInfo;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 冲突策略自动检测器 Automatically detects the best conflict resolution strategy based on table structure
 */
public class ConflictStrategyDetector {

    private static final Logger logger = LoggerFactory.getLogger(ConflictStrategyDetector.class);

    private final JdbcService jdbcService;

    // 常见的版本号字段名
    private static final List<String> VERSION_COLUMN_NAMES =
            Arrays.asList("version", "row_version", "revision", "ver", "record_version");

    // 常见的更新时间字段名
    private static final List<String> UPDATE_TIME_COLUMN_NAMES =
            Arrays.asList(
                    "update_time",
                    "updated_at",
                    "modified_at",
                    "modify_time",
                    "last_modified",
                    "last_update_time",
                    "updatetime");

    // 常见的创建时间字段名
    private static final List<String> CREATE_TIME_COLUMN_NAMES =
            Arrays.asList("create_time", "created_at", "insert_time", "createtime");

    public ConflictStrategyDetector(JdbcService jdbcService) {
        if (jdbcService == null) {
            throw new IllegalArgumentException("JdbcService cannot be null");
        }
        this.jdbcService = jdbcService;
    }

    /**
     * 自动检测并配置最佳冲突策略 Automatically detect and configure the best conflict strategy
     *
     * @param config 映射配置
     * @return 检测结果
     */
    public StrategyDetectionResult detectAndConfigure(MappingConfiguration config) {
        if (config.getConflictStrategy() != ConflictStrategy.AUTO) {
            // 如果不是AUTO策略，直接返回当前配置
            return new StrategyDetectionResult(
                    config.getConflictStrategy(),
                    "Strategy explicitly set to " + config.getConflictStrategy(),
                    true);
        }

        logger.info(
                "Auto-detecting conflict strategy for table: {}", config.getQualifiedTableName());

        try {
            // 获取表的所有列信息
            Map<String, ColumnInfo> columns =
                    getTableColumns(config.getSchemaName(), config.getTableName());

            if (columns.isEmpty()) {
                logger.warn(
                        "No columns found for table {}, using FILE_WINS as fallback",
                        config.getQualifiedTableName());
                return configureFallback(config);
            }

            // 按优先级检测策略
            StrategyDetectionResult result;

            // 1. 检测版本号字段
            result = detectVersionStrategy(config, columns);
            if (result.isSuccess()) {
                return result;
            }

            // 2. 检测更新时间字段
            result = detectUpdateTimeStrategy(config, columns);
            if (result.isSuccess()) {
                return result;
            }

            // 3. 检测创建时间字段
            result = detectTimestampStrategy(config, columns);
            if (result.isSuccess()) {
                return result;
            }

            // 4. 使用默认策略
            return configureFallback(config);

        } catch (Exception e) {
            logger.error(
                    "Failed to detect conflict strategy for table {}: {}",
                    config.getQualifiedTableName(),
                    e.getMessage(),
                    e);
            return configureFallback(config);
        }
    }

    /** 检测版本号策略 */
    private StrategyDetectionResult detectVersionStrategy(
            MappingConfiguration config, Map<String, ColumnInfo> columns) {

        for (String versionColumnName : VERSION_COLUMN_NAMES) {
            ColumnInfo column = findColumnIgnoreCase(columns, versionColumnName);
            if (column != null && isNumericType(column.getDataType())) {
                // 找到版本号字段
                config.setConflictStrategy(ConflictStrategy.VERSION_WINS);
                config.setVersionColumn(column.getColumnName());

                // 同时设置时间戳字段（如果存在）
                detectAndSetTimeColumns(config, columns);

                logger.info(
                        "Detected VERSION_WINS strategy for table {}: version column = {}",
                        config.getQualifiedTableName(),
                        column.getColumnName());

                return new StrategyDetectionResult(
                        ConflictStrategy.VERSION_WINS,
                        String.format(
                                "Found version column '%s' (type: %s)",
                                column.getColumnName(), column.getDataType()),
                        true);
            }
        }

        return new StrategyDetectionResult(null, "No version column found", false);
    }

    /** 检测更新时间策略 */
    private StrategyDetectionResult detectUpdateTimeStrategy(
            MappingConfiguration config, Map<String, ColumnInfo> columns) {

        for (String updateTimeColumnName : UPDATE_TIME_COLUMN_NAMES) {
            ColumnInfo column = findColumnIgnoreCase(columns, updateTimeColumnName);
            if (column != null && isTimestampType(column.getDataType())) {
                // 找到更新时间字段
                config.setConflictStrategy(ConflictStrategy.UPDATE_TIME_WINS);
                config.setUpdateTimeColumn(column.getColumnName());

                // 同时设置创建时间字段（如果存在）
                detectAndSetCreateTimeColumn(config, columns);

                logger.info(
                        "Detected UPDATE_TIME_WINS strategy for table {}: update_time column = {}",
                        config.getQualifiedTableName(),
                        column.getColumnName());

                return new StrategyDetectionResult(
                        ConflictStrategy.UPDATE_TIME_WINS,
                        String.format(
                                "Found update_time column '%s' (type: %s)",
                                column.getColumnName(), column.getDataType()),
                        true);
            }
        }

        return new StrategyDetectionResult(null, "No update_time column found", false);
    }

    /** 检测时间戳策略 */
    private StrategyDetectionResult detectTimestampStrategy(
            MappingConfiguration config, Map<String, ColumnInfo> columns) {

        for (String createTimeColumnName : CREATE_TIME_COLUMN_NAMES) {
            ColumnInfo column = findColumnIgnoreCase(columns, createTimeColumnName);
            if (column != null && isTimestampType(column.getDataType())) {
                // 找到创建时间字段
                config.setConflictStrategy(ConflictStrategy.TIMESTAMP_WINS);
                config.setCreateTimeColumn(column.getColumnName());

                logger.info(
                        "Detected TIMESTAMP_WINS strategy for table {}: created_at column = {}",
                        config.getQualifiedTableName(),
                        column.getColumnName());

                return new StrategyDetectionResult(
                        ConflictStrategy.TIMESTAMP_WINS,
                        String.format(
                                "Found created_at column '%s' (type: %s)",
                                column.getColumnName(), column.getDataType()),
                        true);
            }
        }

        return new StrategyDetectionResult(null, "No timestamp column found", false);
    }

    /** 配置后备策略 */
    private StrategyDetectionResult configureFallback(MappingConfiguration config) {
        config.setConflictStrategy(ConflictStrategy.FILE_WINS);

        logger.info(
                "Using fallback strategy FILE_WINS for table {}", config.getQualifiedTableName());

        return new StrategyDetectionResult(
                ConflictStrategy.FILE_WINS,
                "No version control columns found, using FILE_WINS as fallback",
                true);
    }

    /** 检测并设置时间戳字段 */
    private void detectAndSetTimeColumns(
            MappingConfiguration config, Map<String, ColumnInfo> columns) {
        // 设置更新时间字段
        if (config.getUpdateTimeColumn() == null) {
            detectAndSetUpdateTimeColumn(config, columns);
        }

        // 设置创建时间字段
        if (config.getCreateTimeColumn() == null) {
            detectAndSetCreateTimeColumn(config, columns);
        }
    }

    /** 检测并设置更新时间字段 */
    private void detectAndSetUpdateTimeColumn(
            MappingConfiguration config, Map<String, ColumnInfo> columns) {
        for (String updateTimeColumnName : UPDATE_TIME_COLUMN_NAMES) {
            ColumnInfo column = findColumnIgnoreCase(columns, updateTimeColumnName);
            if (column != null && isTimestampType(column.getDataType())) {
                config.setUpdateTimeColumn(column.getColumnName());
                logger.debug("Detected update_time column: {}", column.getColumnName());
                return;
            }
        }
    }

    /** 检测并设置创建时间字段 */
    private void detectAndSetCreateTimeColumn(
            MappingConfiguration config, Map<String, ColumnInfo> columns) {
        for (String createTimeColumnName : CREATE_TIME_COLUMN_NAMES) {
            ColumnInfo column = findColumnIgnoreCase(columns, createTimeColumnName);
            if (column != null && isTimestampType(column.getDataType())) {
                config.setCreateTimeColumn(column.getColumnName());
                logger.debug("Detected create_time column: {}", column.getColumnName());
                return;
            }
        }
    }

    /** 获取表的所有列信息 */
    private Map<String, ColumnInfo> getTableColumns(String schemaName, String tableName) {
        Map<String, ColumnInfo> columns = new HashMap<>();

        try {
            String qualifiedTableName = schemaName + "." + tableName;
            List<TableColumnInfo> tableFields =
                    jdbcService.getTableFields(schemaName, qualifiedTableName);

            for (TableColumnInfo field : tableFields) {
                String columnName = field.getColumnName();
                String dataType = field.getDataType();
                // TableColumnInfo 没有 sqlType，使用 0 作为占位符
                ColumnInfo columnInfo = new ColumnInfo(columnName, dataType, 0);
                columns.put(columnName.toLowerCase(), columnInfo);
            }
        } catch (Exception e) {
            logger.error(
                    "Failed to get table columns for {}.{}: {}",
                    schemaName,
                    tableName,
                    e.getMessage(),
                    e);
            throw new RuntimeException("Failed to get table columns: " + e.getMessage(), e);
        }

        return columns;
    }

    /** 忽略大小写查找列 */
    private ColumnInfo findColumnIgnoreCase(Map<String, ColumnInfo> columns, String columnName) {
        return columns.get(columnName.toLowerCase());
    }

    /** 判断是否为数字类型 */
    private boolean isNumericType(String dataType) {
        String type = dataType.toUpperCase();
        return type.contains("INT")
                || type.contains("BIGINT")
                || type.contains("SMALLINT")
                || type.contains("NUMERIC")
                || type.contains("DECIMAL")
                || type.equals("NUMBER");
    }

    /** 判断是否为时间戳类型 */
    private boolean isTimestampType(String dataType) {
        String type = dataType.toUpperCase();
        return type.contains("TIMESTAMP") || type.contains("DATETIME") || type.equals("DATE");
    }

    /** 列信息类 */
    private static class ColumnInfo {
        private final String columnName;
        private final String dataType;
        private final int sqlType;

        public ColumnInfo(String columnName, String dataType, int sqlType) {
            this.columnName = columnName;
            this.dataType = dataType;
            this.sqlType = sqlType;
        }

        public String getColumnName() {
            return columnName;
        }

        public String getDataType() {
            return dataType;
        }

        public int getSqlType() {
            return sqlType;
        }
    }

    /** 策略检测结果类 */
    public static class StrategyDetectionResult {
        private final ConflictStrategy detectedStrategy;
        private final String description;
        private final boolean success;

        public StrategyDetectionResult(
                ConflictStrategy detectedStrategy, String description, boolean success) {
            this.detectedStrategy = detectedStrategy;
            this.description = description;
            this.success = success;
        }

        public ConflictStrategy getDetectedStrategy() {
            return detectedStrategy;
        }

        public String getDescription() {
            return description;
        }

        public boolean isSuccess() {
            return success;
        }

        @Override
        public String toString() {
            return String.format(
                    "StrategyDetectionResult{strategy=%s, description='%s', success=%s}",
                    detectedStrategy, description, success);
        }
    }
}
