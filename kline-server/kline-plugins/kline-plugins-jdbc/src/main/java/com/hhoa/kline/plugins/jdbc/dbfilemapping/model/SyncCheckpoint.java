package com.hhoa.kline.plugins.jdbc.dbfilemapping.model;

import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/** 同步检查点模型 Checkpoint for tracking sync progress and enabling resume functionality */
@Data
@NoArgsConstructor
@EqualsAndHashCode(of = {"schemaName", "tableName"})
public class SyncCheckpoint {

    /** 表名 */
    private String tableName;

    /** Schema名称 */
    private String schemaName;

    /** 最后处理的主键值 */
    private Object lastProcessedPrimaryKey;

    /** 已处理的记录数 */
    private long processedRecords;

    /** 总记录数 */
    private long totalRecords;

    /** 失败的记录数 */
    private long failedRecords;

    /** 检查点创建时间 */
    private LocalDateTime checkpointTime = LocalDateTime.now();

    /** 是否完成 */
    private boolean completed = false;

    public SyncCheckpoint(String schemaName, String tableName) {
        this.schemaName = schemaName;
        this.tableName = tableName;
        this.checkpointTime = LocalDateTime.now();
        this.completed = false;
    }

    /** 获取完全限定的表名 */
    public String getQualifiedTableName() {
        return schemaName + "." + tableName;
    }

    /** 计算进度百分比 */
    public double getProgressPercentage() {
        if (totalRecords == 0) {
            return 0.0;
        }
        return (double) processedRecords / totalRecords * 100.0;
    }

    /** 更新检查点 */
    public void update(Object lastPrimaryKey, long processed, long failed) {
        this.lastProcessedPrimaryKey = lastPrimaryKey;
        this.processedRecords = processed;
        this.failedRecords = failed;
        this.checkpointTime = LocalDateTime.now();
    }
}
