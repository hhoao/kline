package com.hhoa.kline.plugins.jdbc.dbfilemapping.manager;

import com.hhoa.kline.plugins.jdbc.dbfilemapping.model.MappingConfiguration;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.model.ValidationResult;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.validator.ConfigurationValidator;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 配置管理器 Manages loading, validation, and reloading of mapping configurations */
public class ConfigurationManager {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationManager.class);

    private final ConfigurationValidator validator;
    private final Map<String, MappingConfiguration> configurations;
    private final ReadWriteLock lock;

    public ConfigurationManager(ConfigurationValidator validator) {
        this.validator = validator;
        this.configurations = new ConcurrentHashMap<>();
        this.lock = new ReentrantReadWriteLock();
    }

    /**
     * 加载配置列表 Loads a list of configurations, validating each one
     *
     * @param configs list of configurations to load
     * @return map of table names to validation results
     */
    public Map<String, ValidationResult> loadConfigurations(List<MappingConfiguration> configs) {
        lock.writeLock().lock();
        try {
            Map<String, ValidationResult> results = new LinkedHashMap<>();

            if (configs == null || configs.isEmpty()) {
                logger.warn("No configurations provided to load");
                return results;
            }

            logger.info("Loading {} configurations", configs.size());

            for (MappingConfiguration config : configs) {
                String key = getConfigKey(config);

                // Validate configuration
                ValidationResult result = validator.validate(config);
                results.put(key, result);

                if (result.isValid()) {
                    configurations.put(key, config);
                    logger.info("Successfully loaded configuration for {}", key);

                    if (result.hasWarnings()) {
                        logger.warn(
                                "Configuration for {} has warnings: {}",
                                key,
                                String.join(", ", result.getWarnings()));
                    }
                } else {
                    logger.error(
                            "Failed to load configuration for {}: {}",
                            key,
                            String.join(", ", result.getErrors()));
                }
            }

            logger.info(
                    "Loaded {} valid configurations out of {}",
                    configurations.size(),
                    configs.size());

            return results;

        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 重新加载配置 Reloads configurations, replacing existing ones
     *
     * @param configs new list of configurations
     * @return map of table names to validation results
     */
    public Map<String, ValidationResult> reloadConfigurations(List<MappingConfiguration> configs) {
        lock.writeLock().lock();
        try {
            logger.info(
                    "Reloading configurations, clearing {} existing configurations",
                    configurations.size());

            // Clear existing configurations
            configurations.clear();

            // Load new configurations
            return loadConfigurations(configs);

        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 添加或更新单个配置 Adds or updates a single configuration
     *
     * @param config configuration to add or update
     * @return validation result
     */
    public ValidationResult addOrUpdateConfiguration(MappingConfiguration config) {
        lock.writeLock().lock();
        try {
            String key = getConfigKey(config);

            // Validate configuration
            ValidationResult result = validator.validate(config);

            if (result.isValid()) {
                MappingConfiguration existing = configurations.get(key);
                if (existing != null) {
                    logger.info("Updating configuration for {}", key);
                } else {
                    logger.info("Adding new configuration for {}", key);
                }

                configurations.put(key, config);

                if (result.hasWarnings()) {
                    logger.warn(
                            "Configuration for {} has warnings: {}",
                            key,
                            String.join(", ", result.getWarnings()));
                }
            } else {
                logger.error(
                        "Failed to add/update configuration for {}: {}",
                        key,
                        String.join(", ", result.getErrors()));
            }

            return result;

        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 移除配置 Removes a configuration
     *
     * @param schemaName schema name
     * @param tableName table name
     * @return true if configuration was removed, false if not found
     */
    public boolean removeConfiguration(String schemaName, String tableName) {
        lock.writeLock().lock();
        try {
            String key = makeConfigKey(schemaName, tableName);
            MappingConfiguration removed = configurations.remove(key);

            if (removed != null) {
                logger.info("Removed configuration for {}", key);
                return true;
            } else {
                logger.warn("Configuration for {} not found", key);
                return false;
            }

        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 获取配置 Gets a configuration by schema and table name
     *
     * @param schemaName schema name
     * @param tableName table name
     * @return configuration or null if not found
     */
    public MappingConfiguration getConfiguration(String schemaName, String tableName) {
        lock.readLock().lock();
        try {
            String key = makeConfigKey(schemaName, tableName);
            return configurations.get(key);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 获取所有配置 Gets all loaded configurations
     *
     * @return unmodifiable collection of configurations
     */
    public Collection<MappingConfiguration> getAllConfigurations() {
        lock.readLock().lock();
        try {
            return Collections.unmodifiableCollection(new ArrayList<>(configurations.values()));
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 获取配置数量 Gets the number of loaded configurations
     *
     * @return number of configurations
     */
    public int getConfigurationCount() {
        lock.readLock().lock();
        try {
            return configurations.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 检查配置是否存在 Checks if a configuration exists
     *
     * @param schemaName schema name
     * @param tableName table name
     * @return true if configuration exists
     */
    public boolean hasConfiguration(String schemaName, String tableName) {
        lock.readLock().lock();
        try {
            String key = makeConfigKey(schemaName, tableName);
            return configurations.containsKey(key);
        } finally {
            lock.readLock().unlock();
        }
    }

    /** 清除所有配置 Clears all configurations */
    public void clearAll() {
        lock.writeLock().lock();
        try {
            int count = configurations.size();
            configurations.clear();
            logger.info("Cleared {} configurations", count);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 验证所有已加载的配置 Re-validates all loaded configurations
     *
     * @return map of table names to validation results
     */
    public Map<String, ValidationResult> validateAll() {
        lock.readLock().lock();
        try {
            Map<String, ValidationResult> results = new LinkedHashMap<>();

            for (Map.Entry<String, MappingConfiguration> entry : configurations.entrySet()) {
                ValidationResult result = validator.validate(entry.getValue());
                results.put(entry.getKey(), result);

                if (!result.isValid()) {
                    logger.error(
                            "Configuration for {} is no longer valid: {}",
                            entry.getKey(),
                            String.join(", ", result.getErrors()));
                }
            }

            return results;

        } finally {
            lock.readLock().unlock();
        }
    }

    /** 获取配置的唯一键 */
    private String getConfigKey(MappingConfiguration config) {
        if (config == null) {
            return "null";
        }
        return makeConfigKey(config.getSchemaName(), config.getTableName());
    }

    /** 创建配置键 */
    private String makeConfigKey(String schemaName, String tableName) {
        String schema = (schemaName == null || schemaName.trim().isEmpty()) ? "public" : schemaName;
        String table = (tableName == null) ? "null" : tableName;
        return schema + "." + table;
    }
}
