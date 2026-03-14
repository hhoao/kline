package com.hhoa.kline.plugins.jdbc.dbfilemapping.model;

import com.hhoa.kline.plugins.jdbc.dbfilemapping.enums.FileChangeType;
import java.nio.file.Path;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 文件变更事件模型 Represents a file system change event */
@Data
@NoArgsConstructor
public class FileChangeEvent {

    /** 文件路径 */
    private Path filePath;

    /** 变更类型 */
    private FileChangeType changeType;

    /** 时间戳 */
    private LocalDateTime timestamp;

    /** 映射配置 */
    private MappingConfiguration config;

    public FileChangeEvent(Path filePath, FileChangeType changeType, MappingConfiguration config) {
        this.filePath = filePath;
        this.changeType = changeType;
        this.timestamp = LocalDateTime.now();
        this.config = config;
    }

    /** 获取文件名（不含扩展名） */
    public String getFileNameWithoutExtension() {
        if (filePath == null) {
            return null;
        }
        String fileName = filePath.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;
    }

    /** 检查是否为JSON文件 */
    public boolean isJsonFile() {
        if (filePath == null) {
            return false;
        }
        return filePath.toString().toLowerCase().endsWith(".json");
    }
}
