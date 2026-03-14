package com.hhoa.kline.plugins.jdbc.dbfilemapping.engine;

import com.hhoa.kline.plugins.jdbc.dbfilemapping.enums.ConflictStrategy;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.helper.FileSystemHelper;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.helper.RecordFieldFilter;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.model.MappingConfiguration;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.serializer.JsonSerializer;
import com.hhoa.kline.plugins.jdbc.service.JdbcService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 冲突解决器 Resolves conflicts when both file and database are modified simultaneously */
public class ConflictResolver {

    private static final Logger logger = LoggerFactory.getLogger(ConflictResolver.class);

    private final JdbcService jdbcService;
    private final JsonSerializer jsonSerializer;
    private final FileSystemHelper fileSystemHelper;

    public ConflictResolver(
            JdbcService jdbcService,
            JsonSerializer jsonSerializer,
            FileSystemHelper fileSystemHelper) {
        this.jdbcService = jdbcService;
        this.jsonSerializer = jsonSerializer;
        this.fileSystemHelper = fileSystemHelper;
    }

    /**
     * 检测冲突 Detects if there's a conflict between file and database
     *
     * @param filePath 文件路径
     * @param config 映射配置
     * @return 冲突信息，如果没有冲突返回null
     */
    public ConflictInfo detectConflict(Path filePath, MappingConfiguration config)
            throws IOException {
        if (!fileSystemHelper.fileExists(filePath)) {
            return null; // 文件不存在，无冲突
        }

        // 获取文件修改时间
        FileTime fileModifiedTime = Files.getLastModifiedTime(filePath);
        LocalDateTime fileTimestamp =
                LocalDateTime.ofInstant(fileModifiedTime.toInstant(), ZoneId.systemDefault());

        // 提取主键值
        String primaryKeyValue = fileSystemHelper.extractPrimaryKeyFromPath(filePath);

        // 查询数据库记录的修改时间
        LocalDateTime dbTimestamp = getDatabaseRecordTimestamp(config, primaryKeyValue);

        if (dbTimestamp == null) {
            return null; // 数据库记录不存在，无冲突
        }

        // 读取文件内容
        String fileContent = fileSystemHelper.readFile(filePath);
        Map<String, Object> fileData;
        try {
            fileData = jsonSerializer.deserialize(fileContent, config);
        } catch (Exception e) {
            logger.warn(
                    "Failed to deserialize file content for conflict detection: {}",
                    e.getMessage());
            return null; // 无法解析文件，无法检测冲突
        }

        // 查询数据库记录内容
        Map<String, Object> dbData = fetchDatabaseRecord(config, primaryKeyValue);

        // 比较内容是否相同
        if (areRecordsEqual(fileData, dbData)) {
            return null; // 内容相同，无冲突
        }

        // 存在冲突
        ConflictInfo conflict = new ConflictInfo();
        conflict.setFilePath(filePath);
        conflict.setTableName(config.getQualifiedTableName());
        conflict.setPrimaryKey(primaryKeyValue);
        conflict.setFileTimestamp(fileTimestamp);
        conflict.setDatabaseTimestamp(dbTimestamp);
        conflict.setFileData(fileData);
        conflict.setDatabaseData(dbData);

        logger.warn(
                "Conflict detected for table {} with primary key {}: file modified at {}, database modified at {}",
                config.getQualifiedTableName(),
                primaryKeyValue,
                fileTimestamp,
                dbTimestamp);

        return conflict;
    }

    /**
     * 解决冲突 Resolves conflict according to the configured strategy
     *
     * @param conflict 冲突信息
     * @param config 映射配置
     * @return 冲突解决结果
     */
    public ConflictResolution resolveConflict(ConflictInfo conflict, MappingConfiguration config)
            throws SyncException {
        if (conflict == null) {
            throw new IllegalArgumentException("Conflict cannot be null");
        }

        ConflictStrategy strategy = config.getConflictStrategy();

        // 如果是AUTO策略，这里不应该到达（应该在初始化时已经转换）
        // 但为了安全，添加一个后备处理
        if (strategy == ConflictStrategy.AUTO) {
            logger.warn(
                    "AUTO strategy should have been resolved during initialization, using FILE_WINS as fallback");
            strategy = ConflictStrategy.FILE_WINS;
        }

        ConflictResolution resolution = new ConflictResolution();
        resolution.setConflictInfo(conflict);
        resolution.setStrategy(strategy);
        resolution.setTimestamp(LocalDateTime.now());

        try {
            switch (strategy) {
                case FILE_WINS:
                    resolveFileWins(conflict, config, resolution);
                    break;

                case DATABASE_WINS:
                    resolveDatabaseWins(conflict, config, resolution);
                    break;

                case TIMESTAMP_WINS:
                    resolveTimestampWins(conflict, config, resolution);
                    break;

                case VERSION_WINS:
                    resolveVersionWins(conflict, config, resolution);
                    break;

                case UPDATE_TIME_WINS:
                    resolveUpdateTimeWins(conflict, config, resolution);
                    break;

                case FETCH_BEFORE_UPDATE:
                    resolveFetchBeforeUpdate(conflict, config, resolution);
                    break;

                default:
                    throw new SyncException("Unknown conflict strategy: " + strategy);
            }

            // 记录冲突解决详情
            logConflictResolution(resolution);

            return resolution;

        } catch (Exception e) {
            resolution.setSuccess(false);
            resolution.setErrorMessage(e.getMessage());

            logger.error(
                    "Failed to resolve conflict for table {} with primary key {}: {}",
                    conflict.getTableName(),
                    conflict.getPrimaryKey(),
                    e.getMessage(),
                    e);

            throw new SyncException("Failed to resolve conflict", e);
        }
    }

    /** FILE_WINS策略：文件内容覆盖数据库 */
    private void resolveFileWins(
            ConflictInfo conflict, MappingConfiguration config, ConflictResolution resolution)
            throws SyncException {
        logger.info(
                "Resolving conflict with FILE_WINS strategy for table {} with primary key {}",
                conflict.getTableName(),
                conflict.getPrimaryKey());

        try {
            // 使用文件数据更新数据库
            updateDatabaseRecord(config, conflict.getFileData(), conflict.getPrimaryKey());

            resolution.setSuccess(true);
            resolution.setWinningSource("FILE");
            resolution.setDescription("File content overwrote database record");

        } catch (Exception e) {
            throw new SyncException("Failed to apply FILE_WINS strategy", e);
        }
    }

    /** DATABASE_WINS策略：数据库内容覆盖文件 */
    private void resolveDatabaseWins(
            ConflictInfo conflict, MappingConfiguration config, ConflictResolution resolution)
            throws SyncException {
        logger.info(
                "Resolving conflict with DATABASE_WINS strategy for table {} with primary key {}",
                conflict.getTableName(),
                conflict.getPrimaryKey());

        try {
            // 使用数据库数据更新文件
            Map<String, Object> filtered =
                    RecordFieldFilter.applyForSerialization(conflict.getDatabaseData(), config);
            String jsonContent = jsonSerializer.serialize(filtered, config);
            fileSystemHelper.writeFile(conflict.getFilePath(), jsonContent);

            resolution.setSuccess(true);
            resolution.setWinningSource("DATABASE");
            resolution.setDescription("Database record overwrote file content");

        } catch (Exception e) {
            throw new SyncException("Failed to apply DATABASE_WINS strategy", e);
        }
    }

    /** TIMESTAMP_WINS策略：使用最新时间戳的数据 */
    private void resolveTimestampWins(
            ConflictInfo conflict, MappingConfiguration config, ConflictResolution resolution)
            throws SyncException {
        logger.info(
                "Resolving conflict with TIMESTAMP_WINS strategy for table {} with primary key {}",
                conflict.getTableName(),
                conflict.getPrimaryKey());

        try {
            // 比较时间戳
            if (conflict.getFileTimestamp().isAfter(conflict.getDatabaseTimestamp())) {
                // 文件更新，使用文件数据
                updateDatabaseRecord(config, conflict.getFileData(), conflict.getPrimaryKey());

                resolution.setSuccess(true);
                resolution.setWinningSource("FILE");
                resolution.setDescription(
                        String.format(
                                "File timestamp (%s) is newer than database timestamp (%s), file content overwrote database",
                                conflict.getFileTimestamp(), conflict.getDatabaseTimestamp()));

            } else {
                // 数据库更新，使用数据库数据
                Map<String, Object> filtered =
                        RecordFieldFilter.applyForSerialization(conflict.getDatabaseData(), config);
                String jsonContent = jsonSerializer.serialize(filtered, config);
                fileSystemHelper.writeFile(conflict.getFilePath(), jsonContent);

                resolution.setSuccess(true);
                resolution.setWinningSource("DATABASE");
                resolution.setDescription(
                        String.format(
                                "Database timestamp (%s) is newer than or equal to file timestamp (%s), database overwrote file",
                                conflict.getDatabaseTimestamp(), conflict.getFileTimestamp()));
            }

        } catch (Exception e) {
            throw new SyncException("Failed to apply TIMESTAMP_WINS strategy", e);
        }
    }

    /** VERSION_WINS策略：使用版本号判断，版本号大的获胜（乐观锁） */
    private void resolveVersionWins(
            ConflictInfo conflict, MappingConfiguration config, ConflictResolution resolution)
            throws SyncException {
        logger.info(
                "Resolving conflict with VERSION_WINS strategy for table {} with primary key {}",
                conflict.getTableName(),
                conflict.getPrimaryKey());

        String versionColumn = config.getVersionColumn();
        if (versionColumn == null || versionColumn.trim().isEmpty()) {
            throw new SyncException(
                    "VERSION_WINS strategy requires versionColumn to be configured");
        }

        try {
            // 获取文件和数据库的版本号
            Object fileVersion = conflict.getFileData().get(versionColumn);
            Object dbVersion = conflict.getDatabaseData().get(versionColumn);

            if (fileVersion == null || dbVersion == null) {
                throw new SyncException(
                        String.format(
                                "Version column '%s' not found in file or database data",
                                versionColumn));
            }

            // 比较版本号
            long fileVersionNum = convertToLong(fileVersion);
            long dbVersionNum = convertToLong(dbVersion);

            if (fileVersionNum > dbVersionNum) {
                // 文件版本更新，使用文件数据并递增版本号
                Map<String, Object> updatedData = new java.util.HashMap<>(conflict.getFileData());
                updatedData.put(versionColumn, fileVersionNum + 1);
                updateDatabaseRecord(config, updatedData, conflict.getPrimaryKey());

                resolution.setSuccess(true);
                resolution.setWinningSource("FILE");
                resolution.setDescription(
                        String.format(
                                "File version (%d) is newer than database version (%d), file content overwrote database with version incremented to %d",
                                fileVersionNum, dbVersionNum, fileVersionNum + 1));

            } else if (dbVersionNum > fileVersionNum) {
                // 数据库版本更新，拉取最新数据到文件
                Map<String, Object> filtered =
                        RecordFieldFilter.applyForSerialization(conflict.getDatabaseData(), config);
                String jsonContent = jsonSerializer.serialize(filtered, config);
                fileSystemHelper.writeFile(conflict.getFilePath(), jsonContent);

                resolution.setSuccess(true);
                resolution.setWinningSource("DATABASE");
                resolution.setDescription(
                        String.format(
                                "Database version (%d) is newer than file version (%d), database overwrote file",
                                dbVersionNum, fileVersionNum));

            } else {
                // 版本号相同但内容不同，这是异常情况，使用数据库数据
                logger.warn("Version numbers are equal but content differs, using database data");
                Map<String, Object> filtered =
                        RecordFieldFilter.applyForSerialization(conflict.getDatabaseData(), config);
                String jsonContent = jsonSerializer.serialize(filtered, config);
                fileSystemHelper.writeFile(conflict.getFilePath(), jsonContent);

                resolution.setSuccess(true);
                resolution.setWinningSource("DATABASE");
                resolution.setDescription(
                        String.format(
                                "Version numbers are equal (%d) but content differs, database overwrote file as safety measure",
                                dbVersionNum));
            }

        } catch (Exception e) {
            throw new SyncException("Failed to apply VERSION_WINS strategy", e);
        }
    }

    /** UPDATE_TIME_WINS策略：使用update_time字段判断 */
    private void resolveUpdateTimeWins(
            ConflictInfo conflict, MappingConfiguration config, ConflictResolution resolution)
            throws SyncException {
        logger.info(
                "Resolving conflict with UPDATE_TIME_WINS strategy for table {} with primary key {}",
                conflict.getTableName(),
                conflict.getPrimaryKey());

        String updateTimeColumn = config.getUpdateTimeColumn();
        if (updateTimeColumn == null || updateTimeColumn.trim().isEmpty()) {
            throw new SyncException(
                    "UPDATE_TIME_WINS strategy requires updateTimeColumn to be configured");
        }

        try {
            // 获取文件和数据库的更新时间
            Object fileUpdateTime = conflict.getFileData().get(updateTimeColumn);
            Object dbUpdateTime = conflict.getDatabaseData().get(updateTimeColumn);

            if (fileUpdateTime == null || dbUpdateTime == null) {
                throw new SyncException(
                        String.format(
                                "Update time column '%s' not found in file or database data",
                                updateTimeColumn));
            }

            // 转换为LocalDateTime进行比较
            LocalDateTime fileTime = convertToLocalDateTime(fileUpdateTime);
            LocalDateTime dbTime = convertToLocalDateTime(dbUpdateTime);

            if (fileTime.isAfter(dbTime)) {
                // 文件更新时间更新，使用文件数据
                updateDatabaseRecord(config, conflict.getFileData(), conflict.getPrimaryKey());

                resolution.setSuccess(true);
                resolution.setWinningSource("FILE");
                resolution.setDescription(
                        String.format(
                                "File update_time (%s) is newer than database update_time (%s), file content overwrote database",
                                fileTime, dbTime));

            } else {
                // 数据库更新时间更新或相等，使用数据库数据
                Map<String, Object> filtered =
                        RecordFieldFilter.applyForSerialization(conflict.getDatabaseData(), config);
                String jsonContent = jsonSerializer.serialize(filtered, config);
                fileSystemHelper.writeFile(conflict.getFilePath(), jsonContent);

                resolution.setSuccess(true);
                resolution.setWinningSource("DATABASE");
                resolution.setDescription(
                        String.format(
                                "Database update_time (%s) is newer than or equal to file update_time (%s), database overwrote file",
                                dbTime, fileTime));
            }

        } catch (Exception e) {
            throw new SyncException("Failed to apply UPDATE_TIME_WINS strategy", e);
        }
    }

    /** FETCH_BEFORE_UPDATE策略：更新前总是先拉取最新版本 这是一种悲观策略，确保总是基于最新数据进行更新 */
    private void resolveFetchBeforeUpdate(
            ConflictInfo conflict, MappingConfiguration config, ConflictResolution resolution)
            throws SyncException {
        logger.info(
                "Resolving conflict with FETCH_BEFORE_UPDATE strategy for table {} with primary key {}",
                conflict.getTableName(),
                conflict.getPrimaryKey());

        try {
            // 1. 先从数据库拉取最新数据
            Map<String, Object> latestDbData =
                    fetchDatabaseRecord(config, conflict.getPrimaryKey());

            if (latestDbData == null) {
                throw new SyncException("Database record not found during fetch");
            }

            // 2. 合并文件的修改到最新数据上
            Map<String, Object> mergedData = new java.util.HashMap<>(latestDbData);

            // 只更新文件中修改的字段（与原数据库数据不同的字段）
            for (Map.Entry<String, Object> entry : conflict.getFileData().entrySet()) {
                String fieldName = entry.getKey();
                Object fileValue = entry.getValue();
                Object originalDbValue = conflict.getDatabaseData().get(fieldName);

                // 如果文件值与原数据库值不同，说明这是用户修改的字段
                if (!java.util.Objects.equals(fileValue, originalDbValue)) {
                    mergedData.put(fieldName, fileValue);
                }
            }

            // 3. 如果配置了版本字段，递增版本号
            if (config.getVersionColumn() != null && !config.getVersionColumn().trim().isEmpty()) {
                Object currentVersion = latestDbData.get(config.getVersionColumn());
                if (currentVersion != null) {
                    long versionNum = convertToLong(currentVersion);
                    mergedData.put(config.getVersionColumn(), versionNum + 1);
                }
            }

            // 4. 更新数据库
            updateDatabaseRecord(config, mergedData, conflict.getPrimaryKey());

            // 5. 同步更新后的数据回文件
            Map<String, Object> filtered =
                    RecordFieldFilter.applyForSerialization(mergedData, config);
            String jsonContent = jsonSerializer.serialize(filtered, config);
            fileSystemHelper.writeFile(conflict.getFilePath(), jsonContent);

            resolution.setSuccess(true);
            resolution.setWinningSource("MERGED");
            resolution.setDescription(
                    "Fetched latest database version, merged file changes, updated database, and synced back to file");

        } catch (Exception e) {
            throw new SyncException("Failed to apply FETCH_BEFORE_UPDATE strategy", e);
        }
    }

    /** 记录冲突解决详情 */
    private void logConflictResolution(ConflictResolution resolution) {
        ConflictInfo conflict = resolution.getConflictInfo();

        logger.info(
                "Conflict resolved for table {} with primary key {}: strategy={}, winner={}, description={}",
                conflict.getTableName(),
                conflict.getPrimaryKey(),
                resolution.getStrategy(),
                resolution.getWinningSource(),
                resolution.getDescription());
    }

    /** 获取数据库记录的修改时间 注意：这需要表中有一个记录修改时间的列（如updated_at） 如果表中没有这样的列，返回当前时间作为近似值 */
    private LocalDateTime getDatabaseRecordTimestamp(
            MappingConfiguration config, Object primaryKey) {
        try {
            // 先查询记录是否存在
            Map<String, Object> record =
                    jdbcService.queryByPrimaryKey(
                            config.getQualifiedTableName(),
                            config.getPrimaryKeyColumn(),
                            primaryKey);

            if (record == null || record.isEmpty()) {
                return null;
            }

            // 尝试获取updated_at或created_at字段
            Object timestamp = record.get("updated_at");
            if (timestamp == null) {
                timestamp = record.get("created_at");
            }

            if (timestamp instanceof java.sql.Timestamp) {
                return ((java.sql.Timestamp) timestamp).toLocalDateTime();
            } else if (timestamp instanceof LocalDateTime) {
                return (LocalDateTime) timestamp;
            }

            // 如果无法获取时间戳，返回当前时间
            return LocalDateTime.now();

        } catch (Exception e) {
            logger.warn(
                    "Failed to get database record timestamp, using current time: {}",
                    e.getMessage());
            return LocalDateTime.now();
        }
    }

    /** 查询数据库记录 */
    private Map<String, Object> fetchDatabaseRecord(
            MappingConfiguration config, Object primaryKey) {
        return jdbcService.queryByPrimaryKey(
                config.getQualifiedTableName(), config.getPrimaryKeyColumn(), primaryKey);
    }

    /** 更新数据库记录 */
    private void updateDatabaseRecord(
            MappingConfiguration config, Map<String, Object> record, Object primaryKey) {
        jdbcService.updateRecord(
                config.getQualifiedTableName(), record, config.getPrimaryKeyColumn(), primaryKey);
    }

    /** 比较两个记录是否相等 */
    private boolean areRecordsEqual(Map<String, Object> record1, Map<String, Object> record2) {
        if (record1 == null || record2 == null) {
            return false;
        }

        if (record1.size() != record2.size()) {
            return false;
        }

        for (Map.Entry<String, Object> entry : record1.entrySet()) {
            Object value1 = entry.getValue();
            Object value2 = record2.get(entry.getKey());

            if (value1 == null && value2 == null) {
                continue;
            }

            if (value1 == null || value2 == null) {
                return false;
            }

            if (!value1.equals(value2)) {
                return false;
            }
        }

        return true;
    }

    /** 将对象转换为long类型（用于版本号比较） */
    private long convertToLong(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        } else if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Cannot convert version to long: " + value);
            }
        } else {
            throw new IllegalArgumentException("Unsupported version type: " + value.getClass());
        }
    }

    /** 将对象转换为LocalDateTime（用于时间比较） */
    private LocalDateTime convertToLocalDateTime(Object value) {
        if (value instanceof LocalDateTime) {
            return (LocalDateTime) value;
        } else if (value instanceof java.sql.Timestamp) {
            return ((java.sql.Timestamp) value).toLocalDateTime();
        } else if (value instanceof java.util.Date) {
            return LocalDateTime.ofInstant(
                    ((java.util.Date) value).toInstant(), ZoneId.systemDefault());
        } else if (value instanceof String) {
            try {
                return LocalDateTime.parse((String) value);
            } catch (Exception e) {
                throw new IllegalArgumentException(
                        "Cannot convert time to LocalDateTime: " + value);
            }
        } else {
            throw new IllegalArgumentException("Unsupported time type: " + value.getClass());
        }
    }

    /** 冲突信息类 */
    @lombok.Data
    public static class ConflictInfo {
        private Path filePath;
        private String tableName;
        private Object primaryKey;
        private LocalDateTime fileTimestamp;
        private LocalDateTime databaseTimestamp;
        private Map<String, Object> fileData;
        private Map<String, Object> databaseData;
    }

    /** 冲突解决结果类 */
    @lombok.Data
    public static class ConflictResolution {
        private ConflictInfo conflictInfo;
        private ConflictStrategy strategy;
        private boolean success;
        private String winningSource; // "FILE" or "DATABASE"
        private String description;
        private String errorMessage;
        private LocalDateTime timestamp;
    }
}
