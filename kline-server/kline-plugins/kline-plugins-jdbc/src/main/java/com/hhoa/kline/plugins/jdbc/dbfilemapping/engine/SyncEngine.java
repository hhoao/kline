package com.hhoa.kline.plugins.jdbc.dbfilemapping.engine;

import com.hhoa.kline.plugins.jdbc.dbfilemapping.model.MappingConfiguration;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.model.SyncCheckpoint;
import java.nio.file.Path;

/**
 * 同步引擎接口 Interface for synchronization engine that handles bidirectional sync between database and
 * files
 */
public interface SyncEngine {

    /**
     * 初始化同步 (数据库 → 文件) Initialize sync by reading all database records and creating files
     *
     * @param config 映射配置
     * @throws SyncException 同步失败时抛出
     */
    void initializeSync(MappingConfiguration config) throws SyncException;

    /**
     * 初始化同步，支持进度监听 Initialize sync with progress listener support
     *
     * @param config 映射配置
     * @param progressListener 进度监听器
     * @throws SyncException 同步失败时抛出
     */
    void initializeSync(MappingConfiguration config, SyncProgressListener progressListener)
            throws SyncException;

    /**
     * 从检查点恢复同步 Resume sync from checkpoint
     *
     * @param config 映射配置
     * @param checkpoint 检查点信息
     * @param progressListener 进度监听器
     * @throws SyncException 同步失败时抛出
     */
    void resumeSync(
            MappingConfiguration config,
            SyncCheckpoint checkpoint,
            SyncProgressListener progressListener)
            throws SyncException;

    /**
     * 同步文件到数据库 Sync file changes to database
     *
     * @param filePath 文件路径
     * @param config 映射配置
     * @throws SyncException 同步失败时抛出
     */
    void syncFileToDatabase(Path filePath, MappingConfiguration config) throws SyncException;

    /**
     * 同步数据库到文件 Sync database record to file
     *
     * @param tableName 表名
     * @param primaryKey 主键值
     * @param config 映射配置
     * @throws SyncException 同步失败时抛出
     */
    void syncDatabaseToFile(String tableName, Object primaryKey, MappingConfiguration config)
            throws SyncException;

    /**
     * 带防循环检查的文件到数据库同步 Sync file to database with circular trigger prevention
     *
     * @param filePath 文件路径
     * @param config 映射配置
     * @param primaryKey 主键值
     * @return true 如果执行了同步，false 如果跳过了同步
     * @throws SyncException 同步失败时抛出
     */
    boolean syncFileToDbWithCheck(Path filePath, MappingConfiguration config, Object primaryKey)
            throws SyncException;

    /**
     * 带防循环检查的数据库到文件同步 Sync database to file with circular trigger prevention
     *
     * @param tableName 表名
     * @param primaryKey 主键值
     * @param config 映射配置
     * @return true 如果执行了同步，false 如果跳过了同步
     * @throws SyncException 同步失败时抛出
     */
    boolean syncDbToFileWithCheck(String tableName, Object primaryKey, MappingConfiguration config)
            throws SyncException;

    /**
     * 处理文件删除 Handle file deletion by deleting corresponding database record
     *
     * @param filePath 文件路径
     * @param config 映射配置
     * @throws SyncException 同步失败时抛出
     */
    void handleFileDeletion(Path filePath, MappingConfiguration config) throws SyncException;

    /**
     * 处理数据库记录删除 Handle database record deletion by deleting corresponding file
     *
     * @param tableName 表名
     * @param primaryKey 主键值
     * @param config 映射配置
     * @throws SyncException 同步失败时抛出
     */
    void handleDatabaseDeletion(String tableName, Object primaryKey, MappingConfiguration config)
            throws SyncException;

    /**
     * 带防循环检查的文件删除处理 Handle file deletion with circular trigger prevention
     *
     * @param filePath 文件路径
     * @param config 映射配置
     * @param primaryKey 主键值
     * @return true 如果执行了删除，false 如果跳过了删除
     * @throws SyncException 删除失败时抛出
     */
    boolean handleFileDeletionWithCheck(
            Path filePath, MappingConfiguration config, Object primaryKey) throws SyncException;

    /**
     * 带防循环检查的数据库删除处理 Handle database deletion with circular trigger prevention
     *
     * @param tableName 表名
     * @param primaryKey 主键值
     * @param config 映射配置
     * @return true 如果执行了删除，false 如果跳过了删除
     * @throws SyncException 删除失败时抛出
     */
    boolean handleDbDeletionWithCheck(
            String tableName, Object primaryKey, MappingConfiguration config) throws SyncException;
}
