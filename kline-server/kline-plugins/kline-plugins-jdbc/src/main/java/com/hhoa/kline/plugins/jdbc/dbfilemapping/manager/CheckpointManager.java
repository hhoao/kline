package com.hhoa.kline.plugins.jdbc.dbfilemapping.manager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.model.SyncCheckpoint;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 检查点管理器 Manages checkpoint persistence for resumable synchronization */
public class CheckpointManager {

    private static final Logger logger = LoggerFactory.getLogger(CheckpointManager.class);
    private static final String CHECKPOINT_DIR = ".sync_checkpoints";
    private static final String CHECKPOINT_FILE_SUFFIX = ".checkpoint.json";

    private final ObjectMapper objectMapper;
    private final String baseDirectory;

    public CheckpointManager(String baseDirectory) {
        this.baseDirectory = baseDirectory;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());

        // 创建检查点目录
        try {
            Path checkpointDir = getCheckpointDirectory();
            if (!Files.exists(checkpointDir)) {
                Files.createDirectories(checkpointDir);
                logger.info("Created checkpoint directory: {}", checkpointDir);
            }
        } catch (IOException e) {
            logger.error("Failed to create checkpoint directory", e);
        }
    }

    /**
     * 保存检查点
     *
     * @param checkpoint 检查点信息
     */
    public void saveCheckpoint(SyncCheckpoint checkpoint) {
        if (checkpoint == null) {
            logger.warn("Cannot save null checkpoint");
            return;
        }

        try {
            Path checkpointFile =
                    getCheckpointFilePath(checkpoint.getSchemaName(), checkpoint.getTableName());
            String json =
                    objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(checkpoint);

            Files.writeString(
                    checkpointFile,
                    json,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);

            logger.debug(
                    "Saved checkpoint for table {}: {} records processed",
                    checkpoint.getQualifiedTableName(),
                    checkpoint.getProcessedRecords());

        } catch (IOException e) {
            logger.error(
                    "Failed to save checkpoint for table {}",
                    checkpoint.getQualifiedTableName(),
                    e);
        }
    }

    /**
     * 加载检查点
     *
     * @param schemaName Schema名称
     * @param tableName 表名
     * @return 检查点信息，如果不存在则返回null
     */
    public SyncCheckpoint loadCheckpoint(String schemaName, String tableName) {
        try {
            Path checkpointFile = getCheckpointFilePath(schemaName, tableName);

            if (!Files.exists(checkpointFile)) {
                logger.debug("No checkpoint found for table {}.{}", schemaName, tableName);
                return null;
            }

            String json = Files.readString(checkpointFile);
            SyncCheckpoint checkpoint = objectMapper.readValue(json, SyncCheckpoint.class);

            logger.info(
                    "Loaded checkpoint for table {}.{}: {} records processed",
                    schemaName,
                    tableName,
                    checkpoint.getProcessedRecords());

            return checkpoint;

        } catch (IOException e) {
            logger.error("Failed to load checkpoint for table {}.{}", schemaName, tableName, e);
            return null;
        }
    }

    /**
     * 删除检查点
     *
     * @param schemaName Schema名称
     * @param tableName 表名
     */
    public void deleteCheckpoint(String schemaName, String tableName) {
        try {
            Path checkpointFile = getCheckpointFilePath(schemaName, tableName);

            if (Files.exists(checkpointFile)) {
                Files.delete(checkpointFile);
                logger.info("Deleted checkpoint for table {}.{}", schemaName, tableName);
            }

        } catch (IOException e) {
            logger.error("Failed to delete checkpoint for table {}.{}", schemaName, tableName, e);
        }
    }

    /**
     * 检查是否存在检查点
     *
     * @param schemaName Schema名称
     * @param tableName 表名
     * @return 如果存在检查点返回true
     */
    public boolean hasCheckpoint(String schemaName, String tableName) {
        Path checkpointFile = getCheckpointFilePath(schemaName, tableName);
        return Files.exists(checkpointFile);
    }

    /** 获取检查点目录 */
    private Path getCheckpointDirectory() {
        return Paths.get(baseDirectory, CHECKPOINT_DIR);
    }

    /** 获取检查点文件路径 */
    private Path getCheckpointFilePath(String schemaName, String tableName) {
        String fileName = schemaName + "." + tableName + CHECKPOINT_FILE_SUFFIX;
        return getCheckpointDirectory().resolve(fileName);
    }
}
