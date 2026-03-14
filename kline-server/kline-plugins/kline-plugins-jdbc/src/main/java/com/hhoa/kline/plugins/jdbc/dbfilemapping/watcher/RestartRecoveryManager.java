package com.hhoa.kline.plugins.jdbc.dbfilemapping.watcher;

import com.hhoa.kline.plugins.jdbc.dbfilemapping.helper.FileSystemHelper;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.helper.MetadataManager;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.model.FileMetadata;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.model.MappingConfiguration;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.model.TableMetadata;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 重启恢复管理器 Manager for handling restart recovery by scanning directories and syncing changes
 *
 * <p>Requirements: 10.5
 */
public class RestartRecoveryManager {

    private static final Logger logger = LoggerFactory.getLogger(RestartRecoveryManager.class);

    private final MetadataManager metadataManager;
    private final FileSystemHelper fileSystemHelper;

    public RestartRecoveryManager(String baseDirectory) {
        this.metadataManager = new MetadataManager(baseDirectory);
        this.fileSystemHelper = new FileSystemHelper();
    }

    /**
     * 执行重启恢复 Scan directory and detect files modified during downtime
     *
     * @param config 映射配置
     * @param callback 文件变更回调
     * @return 检测到的变更文件数量
     */
    public int performRestartRecovery(MappingConfiguration config, FileChangeCallback callback) {
        logger.info(
                "Starting restart recovery for table: {}.{}",
                config.getSchemaName(),
                config.getTableName());

        int changesDetected = 0;

        try {
            // 1. 读取元数据
            TableMetadata tableMetadata =
                    metadataManager.readTableMetadata(
                            config.getSchemaName(), config.getTableName());

            // 2. 扫描目录
            Path tableDirectory =
                    Paths.get(
                            config.getTargetDirectory(),
                            config.getSchemaName(),
                            config.getTableName());

            if (!Files.exists(tableDirectory)) {
                logger.warn("Table directory does not exist: {}", tableDirectory);
                return 0;
            }

            // 3. 检测变更
            List<FileChange> changes = detectChanges(tableDirectory, tableMetadata, config);
            changesDetected = changes.size();

            if (changesDetected == 0) {
                logger.info(
                        "No changes detected during downtime for table: {}.{}",
                        config.getSchemaName(),
                        config.getTableName());
                return 0;
            }

            logger.info(
                    "Detected {} file changes during downtime for table: {}.{}",
                    changesDetected,
                    config.getSchemaName(),
                    config.getTableName());

            // 4. 同步变更
            syncChanges(changes, callback, tableMetadata, config);

            // 5. 更新元数据
            metadataManager.writeTableMetadata(tableMetadata);

            logger.info(
                    "Restart recovery completed for table: {}.{} ({} changes processed)",
                    config.getSchemaName(),
                    config.getTableName(),
                    changesDetected);

        } catch (Exception e) {
            logger.error(
                    "Failed to perform restart recovery for table: {}.{}",
                    config.getSchemaName(),
                    config.getTableName(),
                    e);
        }

        return changesDetected;
    }

    /**
     * 检测文件变更 通过元数据文件判断，支持 SINGLE_JSON 和 FIELD_FILES 两种模式
     *
     * @param tableDirectory 表目录
     * @param tableMetadata 表元数据
     * @param config 映射配置
     * @return 变更列表
     */
    private List<FileChange> detectChanges(
            Path tableDirectory, TableMetadata tableMetadata, MappingConfiguration config)
            throws IOException {
        List<FileChange> changes = new ArrayList<>();
        Set<String> existingRecords = new HashSet<>();

        if (!Files.isDirectory(tableDirectory)) {
            return changes;
        }

        // 扫描表目录，根据文件结构模式判断
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(tableDirectory)) {
            for (Path path : stream) {
                String primaryKey = null;

                // 根据文件结构模式提取主键
                switch (config.getFileStructureMode()) {
                    case SINGLE_JSON:
                        // SINGLE_JSON 模式：主键.json
                        if (Files.isRegularFile(path) && path.toString().endsWith(".json")) {
                            String fileName = path.getFileName().toString();
                            primaryKey = fileName.substring(0, fileName.length() - 5); // 去掉 .json
                        }
                        break;

                    case FIELD_FILES:
                        // FIELD_FILES 模式：主键/字段名
                        if (Files.isDirectory(path)) {
                            primaryKey = path.getFileName().toString();
                        }
                        break;
                }

                if (primaryKey == null) {
                    continue;
                }

                existingRecords.add(primaryKey);
                FileMetadata storedMetadata = tableMetadata.getFileMetadata(primaryKey);

                if (storedMetadata == null) {
                    // 新记录（在停机期间创建）
                    logger.debug(
                            "Detected new record: {} (mode: {})",
                            path,
                            config.getFileStructureMode());
                    changes.add(new FileChange(FileChangeType.CREATED, path, primaryKey));

                } else if (metadataManager.isFileModifiedSinceLastSync(path, storedMetadata)) {
                    // 修改的记录
                    logger.debug(
                            "Detected modified record: {} (mode: {})",
                            path,
                            config.getFileStructureMode());
                    changes.add(new FileChange(FileChangeType.MODIFIED, path, primaryKey));
                }
            }
        }

        // 检测删除的记录（在元数据中但不在目录中）
        for (String primaryKey : tableMetadata.getFileMetadataMap().keySet()) {
            if (!existingRecords.contains(primaryKey)) {
                FileMetadata metadata = tableMetadata.getFileMetadata(primaryKey);
                Path recordPath = Paths.get(metadata.getFilePath());

                logger.debug(
                        "Detected deleted record: {} (mode: {})",
                        recordPath,
                        config.getFileStructureMode());
                changes.add(new FileChange(FileChangeType.DELETED, recordPath, primaryKey));
            }
        }

        return changes;
    }

    /**
     * 同步变更
     *
     * @param changes 变更列表
     * @param callback 文件变更回调
     * @param tableMetadata 表元数据
     * @param config 映射配置
     */
    private void syncChanges(
            List<FileChange> changes,
            FileChangeCallback callback,
            TableMetadata tableMetadata,
            MappingConfiguration config) {
        for (FileChange change : changes) {
            try {
                switch (change.getType()) {
                    case CREATED:
                        logger.debug("Syncing created file: {}", change.getFilePath());
                        callback.onFileCreated(change.getFilePath());

                        // 更新元数据
                        metadataManager.updateFileMetadata(
                                config.getSchemaName(),
                                config.getTableName(),
                                change.getPrimaryKey(),
                                change.getFilePath());
                        break;

                    case MODIFIED:
                        logger.debug("Syncing modified file: {}", change.getFilePath());
                        callback.onFileModified(change.getFilePath());

                        // 更新元数据
                        metadataManager.updateFileMetadata(
                                config.getSchemaName(),
                                config.getTableName(),
                                change.getPrimaryKey(),
                                change.getFilePath());
                        break;

                    case DELETED:
                        logger.debug("Syncing deleted file: {}", change.getFilePath());
                        callback.onFileDeleted(change.getFilePath());

                        // 移除元数据
                        metadataManager.removeFileMetadata(
                                config.getSchemaName(),
                                config.getTableName(),
                                change.getPrimaryKey());
                        break;
                }

            } catch (Exception e) {
                logger.error(
                        "Failed to sync change: {} - {}",
                        change.getType(),
                        change.getFilePath(),
                        e);
            }
        }
    }

    /** 文件变更类型 */
    private enum FileChangeType {
        CREATED,
        MODIFIED,
        DELETED
    }

    /** 文件变更记录 */
    @lombok.Value
    private static class FileChange {
        FileChangeType type;
        Path filePath;
        String primaryKey;
    }
}
