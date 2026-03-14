package com.hhoa.kline.plugins.jdbc.service;

import com.hhoa.kline.plugins.jdbc.enums.ImportModeEnum;
import java.util.List;
import java.util.Map;
import org.springframework.transaction.annotation.Transactional;

/**
 * JdbcService
 *
 * @author xianxing
 * @since 2025/10/22
 */
public interface JdbcService {
    /**
     * 批量插入数据
     *
     * @param tableName 表名
     * @param dataList 数据列表
     * @return 插入成功的记录数
     */
    @Transactional
    int batchInsert(String tableName, List<List<Object>> dataList, List<String> databaseFields);

    /**
     * 批量更新数据
     *
     * @param tableName 表名
     * @param dataList 数据列表
     * @param upsertColumns 用于匹配的字段列表（WHERE条件字段）
     * @param databaseFields 数据库字段列表
     * @return 更新成功的记录数
     */
    @Transactional
    int batchUpdate(
            String tableName,
            List<List<Object>> dataList,
            List<String> upsertColumns,
            List<String> databaseFields);

    /**
     * 批量UPSERT数据（使用ON CONFLICT）
     *
     * @param tableName 表名
     * @param dataList 数据列表
     * @param upsertColumns 冲突字段列表
     * @param databaseFields 数据库字段列表
     * @return UPSERT成功的记录数
     */
    @Transactional
    int batchUpsert(
            String tableName,
            List<List<Object>> dataList,
            List<String> upsertColumns,
            List<String> databaseFields);

    /**
     * 根据导入模式执行批量操作
     *
     * @param tableName 表名
     * @param dataList 数据列表
     * @param importMode 导入模式
     * @param conflictStrategy 冲突策略
     * @param upsertColumns UPSERT模式下的冲突字段列表
     * @param databaseFields 数据库字段列表
     * @return 操作成功的记录数
     */
    @Transactional
    int executeBatchOperation(
            String tableName,
            List<List<Object>> dataList,
            ImportModeEnum importMode,
            String conflictStrategy,
            List<String> upsertColumns,
            List<String> databaseFields);

    /**
     * 获取数据库表的字段信息
     *
     * @param schema
     * @param tableName 表名
     * @return 表字段信息列表
     */
    List<TableColumnInfo> getTableFields(String schema, String tableName);

    /**
     * 获取数据库表的字段名称列表
     *
     * @param tableName 表名
     * @return 表字段名称列表
     */
    List<String> getTableFieldNames(String tableName);

    /**
     * 判断数据库表是否存在
     *
     * @param tableName 表名
     * @return 是否存在
     */
    boolean tableExists(String tableName);

    /**
     * 执行查询并返回单个对象
     *
     * @param sql SQL 查询语句
     * @param elementType 元素类型
     * @param args 查询参数
     * @param <T> 元素类型
     * @return 查询结果对象，如果查询结果为空则返回null
     */
    <T> T queryForObject(String sql, Class<T> elementType, Object... args);

    /**
     * 执行查询并返回指定类型的列表
     *
     * @param sql SQL 查询语句
     * @param elementType 元素类型
     * @param args 查询参数
     * @param <T> 元素类型
     * @return 查询结果列表
     */
    <T> List<T> queryForList(String sql, Class<T> elementType, Object... args);

    /**
     * 执行查询并返回Map列表（支持多列查询）
     *
     * @param sql SQL 查询语句
     * @param args 查询参数
     * @return 查询结果列表，每行数据为一个Map，key为列名，value为列值
     */
    List<Map<String, Object>> queryForMapList(String sql, Object... args);

    /**
     * 根据唯一键批量查询指定字段的值
     *
     * @param tableName 表名
     * @param uniqueKeyColumns 唯一键列名列表
     * @param uniqueKeyValuesList 唯一键值列表，每个元素是一个唯一键的值列表（与uniqueKeyColumns对应）
     * @param targetField 要查询的目标字段名
     * @return 唯一键字符串 -> 目标字段值的映射。唯一键字符串由唯一键值用"|||"连接而成
     */
    Map<String, String> batchQueryByUniqueKeys(
            String tableName,
            List<String> uniqueKeyColumns,
            List<List<Object>> uniqueKeyValuesList,
            String targetField);

    /**
     * 执行更新操作（INSERT、UPDATE、DELETE）
     *
     * @param sql SQL 更新语句
     * @param args 更新参数
     * @return 影响的行数
     */
    int update(String sql, Object... args);

    /**
     * 获取表的总记录数
     *
     * @param tableName 表名（可以是 schema.table 格式）
     * @return 总记录数
     */
    long countRecords(String tableName);

    /**
     * 检查记录是否存在
     *
     * @param tableName 表名（可以是 schema.table 格式）
     * @param primaryKeyColumn 主键列名
     * @param primaryKey 主键值
     * @return 是否存在
     */
    boolean recordExists(String tableName, String primaryKeyColumn, Object primaryKey);

    /**
     * 根据主键查询单条记录
     *
     * @param tableName 表名（可以是 schema.table 格式）
     * @param primaryKeyColumn 主键列名
     * @param primaryKey 主键值
     * @return 记录数据，如果不存在返回null
     */
    Map<String, Object> queryByPrimaryKey(
            String tableName, String primaryKeyColumn, Object primaryKey);

    /**
     * 分页查询记录
     *
     * @param tableName 表名（可以是 schema.table 格式）
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 记录列表
     */
    List<Map<String, Object>> queryWithPagination(String tableName, int offset, int limit);

    /**
     * 插入单条记录
     *
     * @param tableName 表名（可以是 schema.table 格式）
     * @param record 记录数据（Map，key为列名，value为列值）
     * @return 影响的行数
     */
    @Transactional
    int insertRecord(String tableName, Map<String, Object> record);

    /**
     * 更新单条记录
     *
     * @param tableName 表名（可以是 schema.table 格式）
     * @param record 记录数据（Map，key为列名，value为列值）
     * @param primaryKeyColumn 主键列名
     * @param primaryKey 主键值
     * @return 影响的行数
     */
    @Transactional
    int updateRecord(
            String tableName,
            Map<String, Object> record,
            String primaryKeyColumn,
            Object primaryKey);

    /**
     * 更新单个字段
     *
     * @param tableName 表名（可以是 schema.table 格式）
     * @param fieldName 字段名
     * @param fieldValue 字段值
     * @param primaryKeyColumn 主键列名
     * @param primaryKey 主键值
     * @return 影响的行数
     */
    @Transactional
    int updateField(
            String tableName,
            String fieldName,
            Object fieldValue,
            String primaryKeyColumn,
            Object primaryKey);

    /**
     * 删除单条记录
     *
     * @param tableName 表名（可以是 schema.table 格式）
     * @param primaryKeyColumn 主键列名
     * @param primaryKey 主键值
     * @return 影响的行数
     */
    @Transactional
    int deleteRecord(String tableName, String primaryKeyColumn, Object primaryKey);

    /**
     * 获取数据源
     *
     * @return 数据源
     */
    javax.sql.DataSource getDataSource();

    /**
     * 检查 schema 是否存在
     *
     * @param schemaName schema 名称
     * @return 是否存在
     */
    boolean schemaExists(String schemaName);

    /**
     * 检查表是否存在
     *
     * @param schemaName schema 名称
     * @param tableName 表名
     * @return 是否存在
     */
    boolean tableExists(String schemaName, String tableName);

    /**
     * 检查列是否存在
     *
     * @param schemaName schema 名称
     * @param tableName 表名
     * @param columnName 列名
     * @return 是否存在
     */
    boolean columnExists(String schemaName, String tableName, String columnName);

    /**
     * 检查触发器是否存在
     *
     * @param schemaName schema 名称
     * @param tableName 表名
     * @param triggerName 触发器名称
     * @return 是否存在
     */
    boolean triggerExists(String schemaName, String tableName, String triggerName);

    /**
     * 创建通知触发器（PostgreSQL）
     *
     * @param schemaName schema 名称
     * @param tableName 表名
     * @param channel 通知通道名称
     * @param triggerName
     */
    void createNotificationTrigger(
            String schemaName, String tableName, String channel, String triggerName);

    /**
     * 执行 LISTEN 命令（PostgreSQL）
     *
     * @param channel 通道名称
     */
    void executeListen(String channel);

    /**
     * 执行 UNLISTEN 命令（PostgreSQL）
     *
     * @param channel 通道名称
     */
    void executeUnlisten(String channel);

    /** 执行 UNLISTEN * 命令（PostgreSQL） */
    void executeUnlistenAll();

    /**
     * 获取指定 schema 下的所有表名列表
     *
     * @param schemaName schema 名称，如果为 null 或空，则使用当前 schema
     * @return 表名列表
     */
    List<String> getTablesBySchema(String schemaName);

    /**
     * 获取指定 schema 下的所有表信息（含表注释）
     *
     * @param schemaName schema 名称，如果为 null 或空，则使用当前 schema
     * @return 表信息列表（表名、表类型、表注释、schema）
     */
    List<TableInfo> getTablesWithCommentsBySchema(String schemaName);

    /**
     * 执行SQL语句
     *
     * @param sql SQL语句
     * @param readOnly 是否只读模式，只读模式下不能执行更新操作
     * @return SQL执行结果
     */
    Object executeSql(String sql, boolean readOnly);

    /**
     * 删除表中不在指定唯一键值集合中的数据
     *
     * @param tableName 表名
     * @param uniqueKeyColumns 唯一键列名列表
     * @param importUniqueKeyValues 导入数据的唯一键值集合（唯一键值用"|||"连接）
     * @return 删除的记录数
     */
    @Transactional
    int deleteDataNotInUniqueKeyValues(
            String tableName,
            List<String> uniqueKeyColumns,
            java.util.Set<String> importUniqueKeyValues);

    /**
     * 创建临时表用于存储唯一键值
     *
     * @param tempTableName 临时表名
     * @param uniqueKeyColumns 唯一键列名列表
     * @param columnTypes 列类型列表（与uniqueKeyColumns对应）
     * @return 是否创建成功
     */
    @Transactional
    boolean createTempTableForUniqueKeys(
            String tempTableName, List<String> uniqueKeyColumns, List<String> columnTypes);

    /**
     * 批量插入唯一键值到临时表（流式写入）
     *
     * @param tempTableName 临时表名
     * @param uniqueKeyColumns 唯一键列名列表
     * @param uniqueKeyValuesList 唯一键值列表，每个元素是一个唯一键的值列表（与uniqueKeyColumns对应）
     * @return 插入的记录数
     */
    @Transactional
    int batchInsertUniqueKeysToTempTable(
            String tempTableName,
            List<String> uniqueKeyColumns,
            List<List<Object>> uniqueKeyValuesList);

    /**
     * 使用临时表删除表中不在导入数据中的数据
     *
     * @param tableName 目标表名
     * @param tempTableName 临时表名
     * @param uniqueKeyColumns 唯一键列名列表
     * @param excludeConditions 排除条件列表，每个子列表对应uniqueKeyColumns的值，匹配的记录不会被删除
     * @return 删除的记录数
     */
    @Transactional
    int deleteDataNotInTempTable(
            String tableName,
            String tempTableName,
            List<String> uniqueKeyColumns,
            List<List<String>> excludeConditions);

    /**
     * 删除临时表
     *
     * @param tempTableName 临时表名
     */
    @Transactional
    void dropTempTable(String tempTableName);

    /**
     * 清空表（TRUNCATE）
     *
     * @param tableName 表名
     * @return 影响的行数
     */
    @Transactional
    int truncateTable(String tableName);
}
