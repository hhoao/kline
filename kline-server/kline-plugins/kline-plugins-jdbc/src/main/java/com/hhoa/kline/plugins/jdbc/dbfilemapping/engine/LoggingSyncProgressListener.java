package com.hhoa.kline.plugins.jdbc.dbfilemapping.engine;

import com.hhoa.kline.plugins.jdbc.dbfilemapping.model.SyncCheckpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 日志记录的同步进度监听器 Progress listener that logs sync progress to logger */
public class LoggingSyncProgressListener implements SyncProgressListener {

    private static final Logger logger = LoggerFactory.getLogger(LoggingSyncProgressListener.class);

    @Override
    public void onSyncStarted(String tableName, long totalRecords) {
        logger.info(
                "Sync started for table {}: {} total records to process", tableName, totalRecords);
    }

    @Override
    public void onBatchProcessed(
            String tableName, long processedRecords, long totalRecords, int batchSize) {
        double progress = totalRecords > 0 ? (double) processedRecords / totalRecords * 100.0 : 0.0;
        logger.info(
                "Batch processed for table {}: {} records in batch, {}/{} total ({:.2f}%)",
                tableName, batchSize, processedRecords, totalRecords, progress);
    }

    @Override
    public void onCheckpointCreated(SyncCheckpoint checkpoint) {
        logger.info(
                "Checkpoint created for table {}: {} records processed ({:.2f}%)",
                checkpoint.getQualifiedTableName(),
                checkpoint.getProcessedRecords(),
                checkpoint.getProgressPercentage());
    }

    @Override
    public void onSyncCompleted(String tableName, long totalProcessed, long totalFailed) {
        logger.info(
                "Sync completed for table {}: {} records processed successfully, {} failed",
                tableName,
                totalProcessed,
                totalFailed);
    }

    @Override
    public void onError(String tableName, Exception error) {
        logger.error("Error during sync for table {}: {}", tableName, error.getMessage(), error);
    }
}
