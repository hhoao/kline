package com.hhoa.kline.plugins.jdbc.dbfilemapping.manager;

import com.hhoa.kline.plugins.jdbc.converter.PostgresqlDataConverter;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.model.MappingConfiguration;
import com.hhoa.kline.plugins.jdbc.service.JdbcService;
import com.hhoa.kline.plugins.jdbc.service.TableColumnInfo;
import com.hhoa.kline.plugins.jdbc.types.AbstractBaseColumn;
import com.hhoa.kline.plugins.jdbc.types.DataColumnFactory;
import com.hhoa.kline.plugins.jdbc.types.DataType;
import com.hhoa.kline.plugins.jdbc.types.DataTypeConverter;
import com.hhoa.kline.plugins.jdbc.types.TypeConfig;
import com.hhoa.kline.plugins.jdbc.types.TypeConfigUtil;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 表元数据和转换器缓存管理器 统一管理表字段信息和数据类型转换器，供多个组件复用
 *
 * @author Auto-generated
 */
public class TableMetadataCache {

    private static final Logger logger = LoggerFactory.getLogger(TableMetadataCache.class);

    private final JdbcService jdbcService;

    // 缓存表字段信息：表名 -> 字段信息列表
    private final Map<String, List<TableColumnInfo>> tableFieldsCache = new ConcurrentHashMap<>();

    // 缓存字段转换器：表名:字段名 -> 转换器
    private final Map<String, DataTypeConverter> fieldConverterCache = new ConcurrentHashMap<>();

    // 缓存主键转换器：表名:主键列名 -> 转换器
    private final Map<String, DataTypeConverter> primaryKeyConverterCache =
            new ConcurrentHashMap<>();

    // 缓存字段信息映射：表名 -> 字段名 -> TableColumnInfo
    private final Map<String, Map<String, TableColumnInfo>> tableFieldsMapCache =
            new ConcurrentHashMap<>();

    public TableMetadataCache(JdbcService jdbcService) {
        this.jdbcService = jdbcService;
    }

    /**
     * 获取表字段信息（带缓存）
     *
     * @param tableName 表名
     * @return 字段信息列表
     */
    public List<TableColumnInfo> getTableFields(String schema, String tableName) {
        return tableFieldsCache.computeIfAbsent(
                tableName,
                key -> {
                    logger.debug("Loading table fields for table: {}", tableName);
                    List<TableColumnInfo> fields = jdbcService.getTableFields(schema, tableName);

                    // 同时缓存字段映射
                    Map<String, TableColumnInfo> fieldsMap =
                            fields.stream()
                                    .collect(
                                            Collectors.toMap(
                                                    TableColumnInfo::getColumnName,
                                                    Function.identity()));
                    tableFieldsMapCache.put(tableName, fieldsMap);

                    logger.debug("Loaded {} fields for table: {}", fields.size(), tableName);
                    return fields;
                });
    }

    /**
     * 获取字段信息映射（带缓存）
     *
     * @param tableName 表名
     * @return 字段名 -> TableColumnInfo 映射
     */
    public Map<String, TableColumnInfo> getTableFieldsMap(String schema, String tableName) {
        // 确保字段信息已加载
        getTableFields(schema, tableName);
        return tableFieldsMapCache.get(tableName);
    }

    /**
     * 获取指定字段的信息
     *
     * @param tableName 表名
     * @param fieldName 字段名
     * @return 字段信息，如果不存在返回null
     */
    public TableColumnInfo getFieldInfo(String schema, String tableName, String fieldName) {
        Map<String, TableColumnInfo> fieldsMap = getTableFieldsMap(schema, tableName);
        return fieldsMap != null ? fieldsMap.get(fieldName) : null;
    }

    /**
     * 获取或创建字段转换器
     *
     * @param tableName 表名
     * @param fieldName 字段名
     * @return 字段转换器，如果无法创建则返回null
     */
    public DataTypeConverter getFieldConverter(String schema, String tableName, String fieldName) {
        String cacheKey = tableName + ":" + fieldName;

        return fieldConverterCache.computeIfAbsent(
                cacheKey,
                key -> {
                    try {
                        TableColumnInfo fieldInfo = getFieldInfo(schema, tableName, fieldName);
                        if (fieldInfo == null) {
                            logger.warn(
                                    "Field '{}' not found in table {}, cannot create converter",
                                    fieldName,
                                    tableName);
                            return null;
                        }

                        return createConverter(fieldInfo.getDataType());
                    } catch (Exception e) {
                        logger.warn(
                                "Failed to create converter for field '{}' in table {}: {}",
                                fieldName,
                                tableName,
                                e.getMessage());
                        return null;
                    }
                });
    }

    /**
     * 获取或创建主键转换器
     *
     * @param config 映射配置
     * @return 主键转换器，如果无法创建则返回null
     */
    public DataTypeConverter getPrimaryKeyConverter(MappingConfiguration config) {
        String cacheKey = config.getQualifiedTableName() + ":" + config.getPrimaryKeyColumn();

        return primaryKeyConverterCache.computeIfAbsent(
                cacheKey,
                key -> {
                    try {
                        TableColumnInfo primaryKeyField =
                                getFieldInfo(
                                        config.getSchemaName(),
                                        config.getTableName(),
                                        config.getPrimaryKeyColumn());

                        if (primaryKeyField == null) {
                            logger.warn(
                                    "Primary key column '{}' not found in table {}, cannot create converter",
                                    config.getPrimaryKeyColumn(),
                                    config.getQualifiedTableName());
                            return null;
                        }

                        String dataType = primaryKeyField.getDataType();
                        logger.debug(
                                "Primary key column '{}' in table {} has data type: {}",
                                config.getPrimaryKeyColumn(),
                                config.getQualifiedTableName(),
                                dataType);

                        return createConverter(dataType);
                    } catch (Exception e) {
                        logger.warn(
                                "Failed to create primary key converter for table {}: {}",
                                config.getQualifiedTableName(),
                                e.getMessage());
                        return null;
                    }
                });
    }

    /**
     * 转换主键值
     *
     * @param config 映射配置
     * @param primaryKeyValue 主键值（可能是字符串）
     * @return 转换后的主键值
     */
    public Object convertPrimaryKey(MappingConfiguration config, String primaryKeyValue) {
        return convertPrimaryKey(config, (Object) primaryKeyValue);
    }

    /**
     * 转换主键值（通用方法）
     *
     * @param config 映射配置
     * @param primaryKeyValue 主键值（任意类型）
     * @return 转换后的主键值
     */
    public Object convertPrimaryKey(MappingConfiguration config, Object primaryKeyValue) {
        if (primaryKeyValue == null) {
            return null;
        }

        try {
            DataTypeConverter converter = getPrimaryKeyConverter(config);

            if (converter == null) {
                // 如果没有转换器，直接返回原值
                return primaryKeyValue;
            }

            // 将主键值转换为字符串，然后通过转换器转换
            String primaryKeyStr = primaryKeyValue.toString();

            // 创建内部转换器（将字符串转换为内部表示）
            DataColumnFactory internalConverter =
                    PostgresqlDataConverter.createInternalConverter(
                            PostgresqlDataConverter.getDataType(
                                            TypeConfigUtil.getTypeConf("VARCHAR", null))
                                    .getLogicalType());

            // 先转换为内部表示（AbstractBaseColumn），再转换为目标类型
            Object internalValue = internalConverter.get(primaryKeyStr);
            if (internalValue instanceof AbstractBaseColumn) {
                return converter.convert((AbstractBaseColumn) internalValue);
            } else {
                logger.warn(
                        "Internal converter returned non-column type: {}",
                        internalValue.getClass());
                return primaryKeyValue;
            }
        } catch (Exception e) {
            logger.warn(
                    "Failed to convert primary key value '{}' for table {}, using original value: {}",
                    primaryKeyValue,
                    config.getQualifiedTableName(),
                    e.getMessage());
            return primaryKeyValue;
        }
    }

    /**
     * 转换字段值
     *
     * @param tableName 表名
     * @param fieldName 字段名
     * @param fieldValue 字段值（任意类型）
     * @return 转换后的字段值
     */
    public Object convertFieldValue(
            String schema, String tableName, String fieldName, Object fieldValue) {
        if (fieldValue == null) {
            return null;
        }

        if (fieldValue instanceof String) {
            fieldValue = ((String) fieldValue).replaceAll("\\s+$", "");
        }

        try {
            DataTypeConverter converter = getFieldConverter(schema, tableName, fieldName);

            if (converter == null) {
                return fieldValue;
            }

            // 创建内部转换器（将字符串转换为内部表示）
            DataColumnFactory internalConverter =
                    PostgresqlDataConverter.createInternalConverter(
                            PostgresqlDataConverter.getDataType(
                                            TypeConfigUtil.getTypeConf("VARCHAR", null))
                                    .getLogicalType());

            // 先转换为内部表示（AbstractBaseColumn），再转换为目标类型
            Object internalValue = internalConverter.get(fieldValue);
            if (internalValue instanceof AbstractBaseColumn) {
                return converter.convert((AbstractBaseColumn) internalValue);
            } else {
                logger.warn(
                        "Internal converter returned non-column type: {}",
                        internalValue.getClass());
                return fieldValue;
            }
        } catch (Exception e) {
            logger.warn(
                    "Failed to convert field value '{}' for field '{}' in table {}, using original value: {}",
                    fieldValue,
                    fieldName,
                    tableName,
                    e.getMessage());
            return fieldValue;
        }
    }

    /**
     * 创建转换器
     *
     * @param dataType 数据类型字符串
     * @return 转换器
     */
    private DataTypeConverter createConverter(String dataType) {
        // 创建类型配置
        TypeConfig typeConfig = TypeConfigUtil.getTypeConf(dataType, null);

        // 获取数据类型
        DataType dataTypeObj = PostgresqlDataConverter.getDataType(typeConfig);

        // 创建外部转换器（将内部表示转换为数据库类型）
        return PostgresqlDataConverter.createExternalConverter(dataTypeObj.getLogicalType(), null);
    }

    /**
     * 清除指定表的缓存
     *
     * @param tableName 表名
     */
    public void clearCache(String tableName) {
        tableFieldsCache.remove(tableName);
        tableFieldsMapCache.remove(tableName);

        // 清除该表的所有字段转换器
        String prefix = tableName + ":";
        fieldConverterCache.entrySet().removeIf(entry -> entry.getKey().startsWith(prefix));
        primaryKeyConverterCache.entrySet().removeIf(entry -> entry.getKey().startsWith(prefix));

        logger.debug("Cleared cache for table: {}", tableName);
    }

    /** 清除所有缓存 */
    public void clearAllCache() {
        tableFieldsCache.clear();
        tableFieldsMapCache.clear();
        fieldConverterCache.clear();
        primaryKeyConverterCache.clear();
        logger.debug("Cleared all cache");
    }
}
