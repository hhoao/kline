package com.hhoa.kline.plugins.jdbc.dbfilemapping.engine;

import com.hhoa.kline.plugins.jdbc.dbfilemapping.model.SyncCheckpoint;

/** 同步进度监听器接口 Listener interface for tracking synchronization progress */
public interface SyncProgressListener {

    /**
     * 当同步开始时调用
     *
     * @param tableName 表名
     * @param totalRecords 总记录数
     */
    void onSyncStarted(String tableName, long totalRecords);

    /**
     * 当处理一批记录后调用
     *
     * @param tableName 表名
     * @param processedRecords 已处理记录数
     * @param totalRecords 总记录数
     * @param batchSize 本批次大小
     */
    void onBatchProcessed(
            String tableName, long processedRecords, long totalRecords, int batchSize);

    /**
     * 当创建检查点时调用
     *
     * @param checkpoint 检查点信息
     */
    void onCheckpointCreated(SyncCheckpoint checkpoint);

    /**
     * 当同步完成时调用
     *
     * @param tableName 表名
     * @param totalProcessed 总处理记录数
     * @param totalFailed 总失败记录数
     */
    void onSyncCompleted(String tableName, long totalProcessed, long totalFailed);

    /**
     * 当发生错误时调用
     *
     * @param tableName 表名
     * @param error 错误信息
     */
    void onError(String tableName, Exception error);
}
