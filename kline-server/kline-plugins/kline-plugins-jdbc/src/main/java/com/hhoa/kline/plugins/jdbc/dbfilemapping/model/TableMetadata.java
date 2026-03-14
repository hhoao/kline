package com.hhoa.kline.plugins.jdbc.dbfilemapping.model;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 表元数据 Metadata for a database table's file synchronization state
 *
 * <p>Requirements: 10.5
 */
@Data
@NoArgsConstructor
public class TableMetadata {

    /** Schema名称 */
    private String schemaName;

    /** 表名 */
    private String tableName;

    /** 最后同步时间 */
    private LocalDateTime lastSyncTime;

    /** 总记录数 */
    private long totalRecords;

    /** 成功同步记录数 */
    private long syncedRecords;

    /** 失败记录数 */
    private long failedRecords;

    /** 文件元数据映射 (primaryKey -> FileMetadata) */
    private Map<String, FileMetadata> fileMetadataMap = new HashMap<>();

    public TableMetadata(String schemaName, String tableName) {
        this.schemaName = schemaName;
        this.tableName = tableName;
        this.fileMetadataMap = new HashMap<>();
    }

    /** 添加或更新文件元数据 */
    public void putFileMetadata(String primaryKey, FileMetadata metadata) {
        this.fileMetadataMap.put(primaryKey, metadata);
    }

    /** 获取文件元数据 */
    public FileMetadata getFileMetadata(String primaryKey) {
        return this.fileMetadataMap.get(primaryKey);
    }

    /** 移除文件元数据 */
    public void removeFileMetadata(String primaryKey) {
        this.fileMetadataMap.remove(primaryKey);
    }
}
