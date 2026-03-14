package com.hhoa.kline.plugins.jdbc.dbfilemapping.helper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.model.FileMetadata;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.model.TableMetadata;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 元数据管理器 Manager for reading and writing metadata files
 *
 * <p>Requirements: 10.5
 */
public class MetadataManager {

    private static final Logger logger = LoggerFactory.getLogger(MetadataManager.class);

    /** 元数据目录名称 */
    private static final String METADATA_DIR = ".sync_metadata";

    /** 元数据文件扩展名 */
    private static final String METADATA_EXTENSION = ".meta";

    /** JSON序列化器 */
    private final ObjectMapper objectMapper;

    /** 基础目录 */
    private final String baseDirectory;

    public MetadataManager(String baseDirectory) {
        this.baseDirectory = baseDirectory;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    /** 获取元数据目录路径 */
    public Path getMetadataDirectory() {
        return Paths.get(baseDirectory, METADATA_DIR);
    }

    /**
     * 获取表元数据文件路径
     *
     * @param schemaName schema名称
     * @param tableName 表名
     * @return 元数据文件路径
     */
    public Path getMetadataFilePath(String schemaName, String tableName) {
        String fileName = schemaName + "." + tableName + METADATA_EXTENSION;
        return getMetadataDirectory().resolve(fileName);
    }

    /**
     * 读取表元数据
     *
     * @param schemaName schema名称
     * @param tableName 表名
     * @return 表元数据，如果不存在则返回新的空元数据
     */
    public TableMetadata readTableMetadata(String schemaName, String tableName) {
        Path metadataPath = getMetadataFilePath(schemaName, tableName);

        if (!Files.exists(metadataPath)) {
            logger.debug("Metadata file does not exist, creating new: {}", metadataPath);
            return new TableMetadata(schemaName, tableName);
        }

        try {
            String content = Files.readString(metadataPath, StandardCharsets.UTF_8);
            TableMetadata metadata = objectMapper.readValue(content, TableMetadata.class);
            logger.debug(
                    "Read metadata from file: {} (files: {})",
                    metadataPath,
                    metadata.getFileMetadataMap().size());
            return metadata;
        } catch (IOException e) {
            logger.error("Failed to read metadata file: {}", metadataPath, e);
            // 返回新的空元数据
            return new TableMetadata(schemaName, tableName);
        }
    }

    /**
     * 写入表元数据
     *
     * @param metadata 表元数据
     */
    public void writeTableMetadata(TableMetadata metadata) {
        if (metadata == null) {
            logger.warn("Cannot write null metadata");
            return;
        }

        Path metadataPath = getMetadataFilePath(metadata.getSchemaName(), metadata.getTableName());

        try {
            // 确保元数据目录存在
            Files.createDirectories(getMetadataDirectory());

            // 序列化并写入
            String content = objectMapper.writeValueAsString(metadata);
            Files.writeString(metadataPath, content, StandardCharsets.UTF_8);

            logger.debug(
                    "Wrote metadata to file: {} (files: {})",
                    metadataPath,
                    metadata.getFileMetadataMap().size());
        } catch (IOException e) {
            logger.error("Failed to write metadata file: {}", metadataPath, e);
        }
    }

    /**
     * 更新文件元数据
     *
     * @param schemaName schema名称
     * @param tableName 表名
     * @param primaryKey 主键值
     * @param filePath 文件路径
     */
    public void updateFileMetadata(
            String schemaName, String tableName, String primaryKey, Path filePath) {
        try {
            // 读取现有元数据
            TableMetadata tableMetadata = readTableMetadata(schemaName, tableName);

            // 创建或更新文件元数据
            FileMetadata fileMetadata = createFileMetadata(primaryKey, filePath);
            tableMetadata.putFileMetadata(primaryKey, fileMetadata);

            // 更新统计信息
            tableMetadata.setLastSyncTime(LocalDateTime.now());
            tableMetadata.setTotalRecords(tableMetadata.getFileMetadataMap().size());

            // 写回
            writeTableMetadata(tableMetadata);

        } catch (Exception e) {
            logger.error(
                    "Failed to update file metadata: {}.{} - {}",
                    schemaName,
                    tableName,
                    primaryKey,
                    e);
        }
    }

    /**
     * 移除文件元数据
     *
     * @param schemaName schema名称
     * @param tableName 表名
     * @param primaryKey 主键值
     */
    public void removeFileMetadata(String schemaName, String tableName, String primaryKey) {
        try {
            // 读取现有元数据
            TableMetadata tableMetadata = readTableMetadata(schemaName, tableName);

            // 移除文件元数据
            tableMetadata.removeFileMetadata(primaryKey);

            // 更新统计信息
            tableMetadata.setLastSyncTime(LocalDateTime.now());
            tableMetadata.setTotalRecords(tableMetadata.getFileMetadataMap().size());

            // 写回
            writeTableMetadata(tableMetadata);

        } catch (Exception e) {
            logger.error(
                    "Failed to remove file metadata: {}.{} - {}",
                    schemaName,
                    tableName,
                    primaryKey,
                    e);
        }
    }

    /**
     * 创建文件元数据
     *
     * @param primaryKey 主键值
     * @param filePath 文件路径
     * @return 文件元数据
     */
    private FileMetadata createFileMetadata(String primaryKey, Path filePath) throws IOException {
        FileMetadata metadata = new FileMetadata();
        metadata.setPrimaryKey(primaryKey);
        metadata.setFilePath(filePath.toString());
        metadata.setLastSyncTime(LocalDateTime.now());

        if (Files.exists(filePath)) {
            // 获取文件最后修改时间
            LocalDateTime lastModified =
                    LocalDateTime.ofInstant(
                            Files.getLastModifiedTime(filePath).toInstant(),
                            ZoneId.systemDefault());
            metadata.setLastModifiedTime(lastModified);

            // 只有当是文件时才计算大小和校验和
            if (Files.isRegularFile(filePath)) {
                // 获取文件大小
                metadata.setFileSize(Files.size(filePath));

                // 计算文件校验和
                String checksum = calculateChecksum(filePath);
                metadata.setChecksum(checksum);
            } else if (Files.isDirectory(filePath)) {
                // 如果是目录，设置为0（表示这是一个记录目录）
                metadata.setFileSize(0L);
                metadata.setChecksum("DIR");
            }
        }

        return metadata;
    }

    /**
     * 计算文件校验和（SHA-256）
     *
     * @param filePath 文件路径
     * @return 校验和（十六进制字符串）
     */
    public String calculateChecksum(Path filePath) throws IOException {
        // 如果是目录，返回特殊标记
        if (Files.isDirectory(filePath)) {
            return "DIR";
        }

        // 如果不是常规文件，返回空字符串
        if (!Files.isRegularFile(filePath)) {
            return "";
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] fileBytes = Files.readAllBytes(filePath);
            byte[] hashBytes = digest.digest(fileBytes);

            // 转换为十六进制字符串
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            logger.error("SHA-256 algorithm not available", e);
            throw new IOException("Failed to calculate checksum", e);
        }
    }

    /**
     * 检查文件是否在停机期间被修改
     *
     * @param filePath 文件路径
     * @param metadata 文件元数据
     * @return true如果文件被修改
     */
    public boolean isFileModifiedSinceLastSync(Path filePath, FileMetadata metadata) {
        if (metadata == null || !Files.exists(filePath)) {
            return false;
        }

        try {
            // 比较文件最后修改时间
            LocalDateTime currentModifiedTime =
                    LocalDateTime.ofInstant(
                            Files.getLastModifiedTime(filePath).toInstant(),
                            ZoneId.systemDefault());

            LocalDateTime lastSyncTime = metadata.getLastSyncTime();
            if (lastSyncTime == null) {
                return true;
            }

            // 如果文件修改时间晚于最后同步时间，则认为被修改
            if (currentModifiedTime.isAfter(lastSyncTime)) {
                logger.debug(
                        "File modified since last sync: {} (current: {}, last sync: {})",
                        filePath,
                        currentModifiedTime,
                        lastSyncTime);
                return true;
            }

            // 额外检查：比较校验和
            String currentChecksum = calculateChecksum(filePath);
            if (!currentChecksum.equals(metadata.getChecksum())) {
                logger.debug(
                        "File checksum changed: {} (current: {}, stored: {})",
                        filePath,
                        currentChecksum,
                        metadata.getChecksum());
                return true;
            }

            return false;

        } catch (IOException e) {
            logger.error("Failed to check if file was modified: {}", filePath, e);
            // 出错时保守处理，认为文件被修改
            return true;
        }
    }
}
