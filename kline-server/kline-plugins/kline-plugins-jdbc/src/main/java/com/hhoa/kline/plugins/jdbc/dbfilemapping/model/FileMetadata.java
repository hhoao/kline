package com.hhoa.kline.plugins.jdbc.dbfilemapping.model;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件元数据 Metadata for tracking file synchronization state
 *
 * <p>Requirements: 10.5
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileMetadata {

    /** 文件路径（相对于表目录） */
    private String filePath;

    /** 主键值 */
    private String primaryKey;

    /** 最后同步时间 */
    private LocalDateTime lastSyncTime;

    /** 文件最后修改时间 */
    private LocalDateTime lastModifiedTime;

    /** 文件内容校验和（用于冲突检测） */
    private String checksum;

    /** 文件大小（字节） */
    private long fileSize;
}
