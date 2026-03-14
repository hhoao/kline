package com.hhoa.kline.plugins.jdbc.dbfilemapping.manager;

import com.hhoa.kline.plugins.jdbc.dbfilemapping.engine.ConflictResolver;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.engine.DefaultSyncEngine;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.engine.ErrorHandler;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.engine.SyncEngine;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.helper.FileSystemHelper;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.listener.DatabaseChangeListener;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.listener.DefaultDatabaseChangeListener;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.logger.SyncLogger;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.model.MappingConfiguration;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.serializer.JacksonJsonSerializer;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.serializer.JsonSerializer;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.validator.ConfigurationValidator;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.watcher.DefaultFileWatcher;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.watcher.FileWatcher;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.watcher.PollingFileWatcher;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.watcher.ResilientFileWatcher;
import com.hhoa.kline.plugins.jdbc.service.JdbcService;
import java.util.List;
import lombok.Getter;

/**
 * DatabaseFileMapper 工厂类 Factory for creating DatabaseFileMapper instances with minimal
 * configuration
 *
 * <p>使用示例：
 *
 * <pre>
 * // 创建工厂
 * DatabaseFileMapperFactory factory = new DatabaseFileMapperFactory(jdbcService);
 *
 * // 创建映射配置
 * MappingConfiguration config = new MappingConfiguration("users", "/workspace/data", "id");
 * config.setSchemaName("public");
 *
 * // 创建 DatabaseFileMapper
 * DatabaseFileMapper mapper = factory.create(config);
 *
 * // 或者批量创建
 * List&lt;MappingConfiguration&gt; configs = Arrays.asList(config1, config2, config3);
 * DatabaseFileMapper mapper = factory.create(configs);
 * </pre>
 */
public class DatabaseFileMapperFactory {

    private final JdbcService jdbcService;
    @Getter private final TableMetadataCache tableMetadataCache;
    private final String metadataDirectory;

    /**
     * 创建工厂实例，指定基础目录
     *
     * @param jdbcService JDBC服务
     * @param metadataDirectory 基础目录路径
     */
    public DatabaseFileMapperFactory(JdbcService jdbcService, String metadataDirectory) {
        if (jdbcService == null) {
            throw new IllegalArgumentException("JdbcService cannot be null");
        }
        if (metadataDirectory == null || metadataDirectory.trim().isEmpty()) {
            throw new IllegalArgumentException("Base directory cannot be null or empty");
        }

        this.jdbcService = jdbcService;
        this.tableMetadataCache = new TableMetadataCache(jdbcService);
        this.metadataDirectory = metadataDirectory;
    }

    /**
     * 创建 DatabaseFileMapper（单个映射配置）
     *
     * @param config 映射配置
     * @return DatabaseFileMapper 实例
     */
    public DatabaseFileMapper create(MappingConfiguration config) {
        if (config == null) {
            throw new IllegalArgumentException("MappingConfiguration cannot be null");
        }
        return create(List.of(config));
    }

    /**
     * 创建 DatabaseFileMapper（多个映射配置）
     *
     * @param configs 映射配置列表
     * @return DatabaseFileMapper 实例
     */
    public DatabaseFileMapper create(List<MappingConfiguration> configs) {
        // 创建所有必需的组件
        JsonSerializer jsonSerializer = createJsonSerializer();
        FileSystemHelper fileSystemHelper = createFileSystemHelper();
        CheckpointManager checkpointManager = createCheckpointManager();
        SyncLogger syncLogger = createSyncLogger();
        ErrorHandler errorHandler = createErrorHandler(syncLogger);
        ConfigurationValidator configurationValidator = createConfigurationValidator();
        ConfigurationManager configurationManager =
                createConfigurationManager(configurationValidator);
        ConflictResolver conflictResolver =
                createConflictResolver(jsonSerializer, fileSystemHelper);
        SyncEngine syncEngine =
                createSyncEngine(jsonSerializer, fileSystemHelper, checkpointManager, errorHandler);
        FileWatcher fileWatcher = createFileWatcher();
        DatabaseChangeListener databaseChangeListener = createDatabaseChangeListener();

        // 创建 DatabaseFileMapper
        DatabaseFileMapper mapper =
                new DefaultDatabaseFileMapper(
                        configurationManager,
                        syncEngine,
                        fileWatcher,
                        databaseChangeListener,
                        syncLogger);

        // 初始化配置
        if (configs != null && !configs.isEmpty()) {
            try {
                mapper.initialize(configs);
            } catch (DatabaseFileMapperException e) {
                throw new RuntimeException("Failed to initialize DatabaseFileMapper", e);
            }
        }

        return mapper;
    }

    // ==================== 组件创建方法 ====================

    private JsonSerializer createJsonSerializer() {
        return new JacksonJsonSerializer();
    }

    private FileSystemHelper createFileSystemHelper() {
        return new FileSystemHelper();
    }

    private CheckpointManager createCheckpointManager() {
        return new CheckpointManager(metadataDirectory);
    }

    private SyncLogger createSyncLogger() {
        return new SyncLogger();
    }

    private ErrorHandler createErrorHandler(SyncLogger syncLogger) {
        return new ErrorHandler(syncLogger);
    }

    private ConfigurationValidator createConfigurationValidator() {
        return new ConfigurationValidator(jdbcService);
    }

    private ConfigurationManager createConfigurationManager(ConfigurationValidator validator) {
        return new ConfigurationManager(validator);
    }

    private ConflictResolver createConflictResolver(
            JsonSerializer jsonSerializer, FileSystemHelper fileSystemHelper) {
        return new ConflictResolver(jdbcService, jsonSerializer, fileSystemHelper);
    }

    private SyncEngine createSyncEngine(
            JsonSerializer jsonSerializer,
            FileSystemHelper fileSystemHelper,
            CheckpointManager checkpointManager,
            ErrorHandler errorHandler) {
        return new DefaultSyncEngine(
                jdbcService,
                jsonSerializer,
                fileSystemHelper,
                checkpointManager,
                errorHandler,
                metadataDirectory,
                tableMetadataCache);
    }

    private FileWatcher createFileWatcher() {
        // 创建基础文件监听器
        DefaultFileWatcher baseWatcher = new DefaultFileWatcher(metadataDirectory);

        // 创建轮询监听器作为降级方案
        PollingFileWatcher fallbackWatcher = new PollingFileWatcher();

        // 使用弹性监听器包装，提供自动降级和恢复能力
        return new ResilientFileWatcher(baseWatcher, fallbackWatcher);
    }

    private DatabaseChangeListener createDatabaseChangeListener() {
        return new DefaultDatabaseChangeListener(jdbcService);
    }
}
