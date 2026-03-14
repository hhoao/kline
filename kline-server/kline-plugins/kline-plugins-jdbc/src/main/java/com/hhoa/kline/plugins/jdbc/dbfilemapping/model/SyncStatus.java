package com.hhoa.kline.plugins.jdbc.dbfilemapping.model;

import com.hhoa.kline.plugins.jdbc.dbfilemapping.enums.SyncState;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/** 同步状态模型 Represents the current synchronization status for a table mapping */
@Data
@NoArgsConstructor
@EqualsAndHashCode(of = {"tableName", "targetDirectory"})
public class SyncStatus {

    /** 表名 */
    private String tableName;

    /** 目标目录 */
    private String targetDirectory;

    /** 同步状态 */
    private SyncState state = SyncState.INITIALIZING;

    /** 最后同步时间 */
    private LocalDateTime lastSyncTime;

    /** 总记录数 */
    private long totalRecords;

    /** 已同步记录数 */
    private long syncedRecords;

    /** 失败记录数 */
    private long failedRecords;

    /** 最近的错误列表 */
    private List<SyncError> recentErrors = new ArrayList<>();

    public SyncStatus(String tableName, String targetDirectory) {
        this.tableName = tableName;
        this.targetDirectory = targetDirectory;
        this.state = SyncState.INITIALIZING;
        this.recentErrors = new ArrayList<>();
    }

    /** 添加错误到最近错误列表 */
    public void addError(SyncError error) {
        if (this.recentErrors == null) {
            this.recentErrors = new ArrayList<>();
        }
        this.recentErrors.add(error);
        this.failedRecords++;
    }

    /** 增加已同步记录数 */
    public void incrementSyncedRecords() {
        this.syncedRecords++;
    }

    /** 计算同步进度百分比 */
    public double getProgressPercentage() {
        if (totalRecords == 0) {
            return 0.0;
        }
        return (double) syncedRecords / totalRecords * 100.0;
    }
}
