package com.hhoa.kline.plugins.jdbc.dbfilemapping.helper;

import com.hhoa.kline.plugins.jdbc.dbfilemapping.enums.FileStructureMode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 文件系统辅助类 Helper class for file system operations */
public class FileSystemHelper {

    private static final Logger logger = LoggerFactory.getLogger(FileSystemHelper.class);

    // 特殊字符替换模式
    private static final Pattern SPECIAL_CHARS_PATTERN = Pattern.compile("[^a-zA-Z0-9._-]");

    /**
     * 创建schema/table目录结构 Create directory structure for schema/table
     *
     * @param baseDirectory 基础目录
     * @param schemaName schema名称
     * @param tableName 表名
     * @return 表目录路径
     * @throws IOException 创建失败时抛出
     */
    public Path createTableDirectory(String baseDirectory, String schemaName, String tableName)
            throws IOException {
        Path basePath = Paths.get(baseDirectory);
        Path schemaPath = basePath.resolve(schemaName);
        Path tablePath = schemaPath.resolve(tableName);

        Files.createDirectories(tablePath);
        logger.debug("Created table directory: {}", tablePath);

        return tablePath;
    }

    /**
     * 生成文件名（基于主键） Generate filename from primary key value
     *
     * @param primaryKeyValue 主键值
     * @return 清理后的文件名（不含扩展名）
     */
    public String generateFileName(Object primaryKeyValue) {
        if (primaryKeyValue == null) {
            throw new IllegalArgumentException("Primary key value cannot be null");
        }

        // 清理特殊字符，替换为下划线
        //        fileName = SPECIAL_CHARS_PATTERN.matcher(fileName).replaceAll("_");

        return primaryKeyValue.toString();
    }

    /**
     * 获取记录目录路径（新结构：表名/主键/） Get directory path for a database record
     *
     * @param baseDirectory 基础目录
     * @param schemaName schema名称
     * @param tableName 表名
     * @param primaryKeyValue 主键值
     * @return 记录目录路径
     */
    public Path getRecordDirectoryPath(
            String baseDirectory, String schemaName, String tableName, Object primaryKeyValue) {
        String primaryKeyDir = generateFileName(primaryKeyValue);
        return Paths.get(baseDirectory, schemaName, tableName, primaryKeyDir);
    }

    /**
     * 获取字段文件路径（新结构：表名/主键/字段名） Get field file path for a database record field
     *
     * @param baseDirectory 基础目录
     * @param schemaName schema名称
     * @param tableName 表名
     * @param primaryKeyValue 主键值
     * @param fieldName 字段名
     * @return 字段文件路径
     */
    public Path getFieldFilePath(
            String baseDirectory,
            String schemaName,
            String tableName,
            Object primaryKeyValue,
            String fieldName) {
        Path recordDir =
                getRecordDirectoryPath(baseDirectory, schemaName, tableName, primaryKeyValue);
        return recordDir.resolve(fieldName);
    }

    /**
     * 获取记录文件路径（旧结构：表名/主键.json）
     *
     * @deprecated 使用新的目录结构 getRecordDirectoryPath
     */
    @Deprecated
    public Path getRecordFilePath(
            String baseDirectory, String schemaName, String tableName, Object primaryKeyValue) {
        String fileName = generateFileName(primaryKeyValue) + ".json";
        return Paths.get(baseDirectory, schemaName, tableName, fileName);
    }

    /**
     * 写入文件内容（带文件锁） Write content to file with file locking
     *
     * @param filePath 文件路径
     * @param content 文件内容
     * @throws IOException 写入失败时抛出
     */
    public void writeFile(Path filePath, String content) throws IOException {
        // 确保父目录存在
        Files.createDirectories(filePath.getParent());

        // 写入文件
        Files.writeString(filePath, content, StandardCharsets.UTF_8);
        logger.debug("Wrote file: {}", filePath);
    }

    /**
     * 读取文件内容 Read content from file
     *
     * @param filePath 文件路径
     * @return 文件内容
     * @throws IOException 读取失败时抛出
     */
    public String readFile(Path filePath) throws IOException {
        if (!Files.exists(filePath)) {
            throw new IOException("File does not exist: " + filePath);
        }

        return Files.readString(filePath, StandardCharsets.UTF_8);
    }

    /**
     * 删除文件 Delete file
     *
     * @param filePath 文件路径
     * @throws IOException 删除失败时抛出
     */
    public void deleteFile(Path filePath) throws IOException {
        if (Files.exists(filePath)) {
            Files.delete(filePath);
            logger.debug("Deleted file: {}", filePath);
        }
    }

    /**
     * 检查文件是否存在 Check if file exists
     *
     * @param filePath 文件路径
     * @return 是否存在
     */
    public boolean fileExists(Path filePath) {
        return Files.exists(filePath);
    }

    /**
     * 从文件路径提取主键值（旧结构） Extract primary key value from file path
     *
     * @param filePath 文件路径
     * @return 主键值（文件名不含扩展名）
     * @deprecated 使用新的目录结构
     */
    @Deprecated
    public String extractPrimaryKeyFromPath(Path filePath) {
        String fileName = filePath.getFileName().toString();
        // 移除.json扩展名
        if (fileName.endsWith(".json")) {
            return fileName.substring(0, fileName.length() - 5);
        }
        return fileName;
    }

    /**
     * 从目录路径提取主键值（新结构） Extract primary key value from directory path
     *
     * @param recordDir 记录目录路径
     * @return 主键值（目录名）
     */
    public String extractPrimaryKeyFromDirectory(Path recordDir) {
        return recordDir.getFileName().toString();
    }

    /**
     * 写入记录的所有字段文件（新结构） Write all field files for a database record
     *
     * @param baseDirectory 基础目录
     * @param schemaName schema名称
     * @param tableName 表名
     * @param primaryKeyValue 主键值
     * @param fields 字段名和值的映射
     * @throws IOException 写入失败时抛出
     */
    public void writeRecordFields(
            String baseDirectory,
            String schemaName,
            String tableName,
            Object primaryKeyValue,
            java.util.Map<String, Object> fields)
            throws IOException {
        Path recordDir =
                getRecordDirectoryPath(baseDirectory, schemaName, tableName, primaryKeyValue);

        // 创建记录目录
        Files.createDirectories(recordDir);

        // 写入每个字段文件
        for (java.util.Map.Entry<String, Object> entry : fields.entrySet()) {
            String fieldName = entry.getKey();
            Object fieldValue = entry.getValue();

            Path fieldFile = recordDir.resolve(fieldName);
            String valueStr = fieldValue == null ? "" : fieldValue.toString();

            Files.writeString(fieldFile, valueStr, StandardCharsets.UTF_8);
        }

        logger.debug("Wrote {} field files to: {}", fields.size(), recordDir);
    }

    /**
     * 读取记录的所有字段文件（新结构） Read all field files for a database record
     *
     * @param baseDirectory 基础目录
     * @param schemaName schema名称
     * @param tableName 表名
     * @param primaryKeyValue 主键值
     * @return 字段名和值的映射
     * @throws IOException 读取失败时抛出
     */
    public java.util.Map<String, String> readRecordFields(
            String baseDirectory, String schemaName, String tableName, Object primaryKeyValue)
            throws IOException {
        Path recordDir =
                getRecordDirectoryPath(baseDirectory, schemaName, tableName, primaryKeyValue);

        if (!Files.exists(recordDir) || !Files.isDirectory(recordDir)) {
            throw new IOException("Record directory does not exist: " + recordDir);
        }

        java.util.Map<String, String> fields = new java.util.HashMap<>();

        // 读取目录下的所有文件
        try (java.util.stream.Stream<Path> files = Files.list(recordDir)) {
            files.filter(Files::isRegularFile)
                    .forEach(
                            fieldFile -> {
                                try {
                                    String fieldName = fieldFile.getFileName().toString();
                                    String fieldValue =
                                            Files.readString(fieldFile, StandardCharsets.UTF_8);
                                    fields.put(fieldName, fieldValue);
                                } catch (IOException e) {
                                    logger.error("Failed to read field file: {}", fieldFile, e);
                                }
                            });
        }

        logger.debug("Read {} field files from: {}", fields.size(), recordDir);
        return fields;
    }

    /**
     * 删除记录目录及其所有字段文件（新结构） Delete record directory and all field files
     *
     * @param baseDirectory 基础目录
     * @param schemaName schema名称
     * @param tableName 表名
     * @param primaryKeyValue 主键值
     * @throws IOException 删除失败时抛出
     */
    public void deleteRecordDirectory(
            String baseDirectory, String schemaName, String tableName, Object primaryKeyValue)
            throws IOException {
        Path recordDir =
                getRecordDirectoryPath(baseDirectory, schemaName, tableName, primaryKeyValue);

        if (Files.exists(recordDir)) {
            // 删除目录及其所有内容
            try (java.util.stream.Stream<Path> files = Files.walk(recordDir)) {
                files.sorted(java.util.Comparator.reverseOrder())
                        .forEach(
                                path -> {
                                    try {
                                        Files.delete(path);
                                    } catch (IOException e) {
                                        logger.error("Failed to delete: {}", path, e);
                                    }
                                });
            }
            logger.debug("Deleted record directory: {}", recordDir);
        }
    }

    /**
     * 检查记录目录是否存在（新结构） Check if record directory exists
     *
     * @param baseDirectory 基础目录
     * @param schemaName schema名称
     * @param tableName 表名
     * @param primaryKeyValue 主键值
     * @return 是否存在
     */
    public boolean recordDirectoryExists(
            String baseDirectory, String schemaName, String tableName, Object primaryKeyValue) {
        Path recordDir =
                getRecordDirectoryPath(baseDirectory, schemaName, tableName, primaryKeyValue);
        return Files.exists(recordDir) && Files.isDirectory(recordDir);
    }

    public Set<String> listRecordPrimaryKeysOnDisk(
            String baseDirectory, String schemaName, String tableName, FileStructureMode mode) {
        Path tablePath = Paths.get(baseDirectory, schemaName, tableName);
        Set<String> primaryKeys = new HashSet<>();

        if (!Files.exists(tablePath) || !Files.isDirectory(tablePath)) {
            return primaryKeys;
        }

        try {
            if (mode == FileStructureMode.SINGLE_JSON) {
                try (var stream = Files.list(tablePath)) {
                    stream.filter(Files::isRegularFile)
                            .filter(p -> p.getFileName().toString().endsWith(".json"))
                            .forEach(
                                    p -> {
                                        String fileName = p.getFileName().toString();
                                        primaryKeys.add(
                                                fileName.substring(0, fileName.length() - 5));
                                    });
                }
            } else {
                try (var stream = Files.list(tablePath)) {
                    stream.filter(Files::isDirectory)
                            .forEach(p -> primaryKeys.add(p.getFileName().toString()));
                }
            }
        } catch (IOException e) {
            logger.error("Failed to list record primary keys from disk: {}", tablePath, e);
        }

        return primaryKeys;
    }
}
