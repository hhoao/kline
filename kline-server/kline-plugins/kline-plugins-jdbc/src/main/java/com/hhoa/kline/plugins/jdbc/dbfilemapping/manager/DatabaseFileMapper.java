package com.hhoa.kline.plugins.jdbc.dbfilemapping.manager;

import com.hhoa.kline.plugins.jdbc.dbfilemapping.model.MappingConfiguration;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.model.SyncStatus;
import java.util.Collection;
import java.util.List;

/**
 * 数据库文件映射器接口 Main controller interface for managing database-file mappings
 *
 * <p>Requirements: 1.1, 1.3, 7.4
 */
public interface DatabaseFileMapper {

    /**
     * 初始化映射器，加载配置并初始化所有组件 Initialize the mapper by loading configurations and initializing all
     * components
     *
     * @param configurations 映射配置列表
     * @throws DatabaseFileMapperException 初始化失败时抛出
     */
    void initialize(List<MappingConfiguration> configurations) throws DatabaseFileMapperException;

    /**
     * 启动所有映射的同步 Start synchronization for all configured mappings
     *
     * @throws DatabaseFileMapperException 启动失败时抛出
     */
    void startAll() throws DatabaseFileMapperException;

    /** 停止所有同步并清理资源 Stop all synchronization and cleanup resources */
    void stopAll();

    /**
     * 获取指定表的同步状态 Get synchronization status for a specific table
     *
     * @param schemaName schema名称
     * @param tableName 表名
     * @return 同步状态，如果映射不存在则返回null
     */
    SyncStatus getSyncStatus(String schemaName, String tableName);

    /**
     * 获取所有映射的同步状态 Get synchronization status for all mappings
     *
     * @return 所有同步状态的集合
     */
    Collection<SyncStatus> getAllSyncStatus();

    /**
     * 手动触发指定表的全量同步 Manually trigger full synchronization for a specific table
     *
     * @param schemaName schema名称
     * @param tableName 表名
     * @throws DatabaseFileMapperException 同步失败时抛出
     */
    void triggerFullSync(String schemaName, String tableName) throws DatabaseFileMapperException;

    /**
     * 重新加载配置 Reload configurations
     *
     * @param configurations 新的配置列表
     * @throws DatabaseFileMapperException 重新加载失败时抛出
     */
    void reloadConfiguration(List<MappingConfiguration> configurations)
            throws DatabaseFileMapperException;

    /**
     * 检查映射器是否正在运行 Check if the mapper is running
     *
     * @return true if running, false otherwise
     */
    boolean isRunning();

    /**
     * 获取已加载的配置数量 Get the number of loaded configurations
     *
     * @return 配置数量
     */
    int getConfigurationCount();
}
