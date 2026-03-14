package com.hhoa.kline.plugins.jdbc.dbfilemapping.model;

import com.hhoa.kline.plugins.jdbc.dbfilemapping.enums.ErrorType;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/** 同步错误模型 Represents an error that occurred during synchronization */
@Data
@NoArgsConstructor
@EqualsAndHashCode(of = {"timestamp", "tableName", "filePath", "errorType"})
public class SyncError {

    /** 错误发生时间 */
    private LocalDateTime timestamp = LocalDateTime.now();

    /** 表名 */
    private String tableName;

    /** 文件路径 */
    private String filePath;

    /** 错误类型 */
    private ErrorType errorType;

    /** 错误消息 */
    private String errorMessage;

    /** 堆栈跟踪 */
    private String stackTrace;

    public SyncError(String tableName, String filePath, ErrorType errorType, String errorMessage) {
        this.timestamp = LocalDateTime.now();
        this.tableName = tableName;
        this.filePath = filePath;
        this.errorType = errorType;
        this.errorMessage = errorMessage;
    }

    public SyncError(
            String tableName,
            String filePath,
            ErrorType errorType,
            String errorMessage,
            String stackTrace) {
        this(tableName, filePath, errorType, errorMessage);
        this.stackTrace = stackTrace;
    }
}
