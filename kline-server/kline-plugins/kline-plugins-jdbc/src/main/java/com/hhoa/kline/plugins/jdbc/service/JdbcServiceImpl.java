package com.hhoa.kline.plugins.jdbc.service;

import com.hhoa.kline.plugins.jdbc.enums.ConflictStrategyEnum;
import com.hhoa.kline.plugins.jdbc.enums.ImportModeEnum;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * JDBC批量操作服务 使用JdbcTemplate执行批量插入、更新、UPSERT操作
 *
 * @author hhoa
 * @date 2025/10/17
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JdbcServiceImpl implements JdbcService {

    private final JdbcTemplate jdbcTemplate;

    @Transactional
    @Override
    public int batchInsert(
            String tableName, List<List<Object>> dataList, List<String> databaseFields) {
        // 默认使用 SKIP 策略
        return batchInsert(tableName, dataList, databaseFields, ConflictStrategyEnum.SKIP);
    }

    /**
     * 批量插入数据（支持冲突策略）
     *
     * @param tableName 表名
     * @param dataList 数据列表
     * @param databaseFields 数据库字段列表
     * @param conflictStrategy 冲突策略
     * @return 插入成功的记录数
     */
    private int batchInsert(
            String tableName,
            List<List<Object>> dataList,
            List<String> databaseFields,
            ConflictStrategyEnum conflictStrategy) {
        if (dataList == null || dataList.isEmpty()) {
            return 0;
        }

        log.info(
                "开始JdbcTemplate批量插入: 表={}, 数据量={}, 冲突策略={}",
                tableName,
                dataList.size(),
                conflictStrategy);

        try {
            // 根据冲突策略构建SQL
            String insertSql =
                    buildInsertSqlWithConflictStrategy(tableName, databaseFields, conflictStrategy);

            // 使用JdbcTemplate的批量操作
            int[][] results =
                    jdbcTemplate.batchUpdate(
                            insertSql,
                            dataList,
                            dataList.size(),
                            (ps, record) -> {
                                for (int i = 0; i < databaseFields.size(); i++) {
                                    Object value = record.get(i);
                                    ps.setObject(i + 1, value);
                                }
                            });

            int successCount = 0;
            for (int[] result : results) {
                for (int updateCount : result) {
                    if (updateCount >= 0) {
                        successCount++;
                    }
                }
            }

            log.info(
                    "JdbcTemplate批量插入完成: 表={}, 成功={}, 总数={}",
                    tableName,
                    successCount,
                    dataList.size());
            return successCount;
        } catch (Exception e) {
            log.error("JdbcTemplate批量插入失败: 表={}, 错误={}", tableName, e.getMessage());
            throw new RuntimeException("批量插入失败: " + e.getMessage(), e);
        }
    }

    @Transactional
    @Override
    public int batchUpdate(
            String tableName,
            List<List<Object>> dataList,
            List<String> upsertColumns,
            List<String> databaseFields) {
        if (dataList == null || dataList.isEmpty()) {
            return 0;
        }

        if (upsertColumns == null || upsertColumns.isEmpty()) {
            throw new IllegalArgumentException("UPDATE模式必须指定匹配字段列表");
        }

        log.info(
                "开始JdbcTemplate批量更新: 表={}, 数据量={}, 匹配字段={}",
                tableName,
                dataList.size(),
                upsertColumns);

        try {
            // 获取第一条记录来构建SQL
            String updateSql = buildUpdateSql(tableName, databaseFields, upsertColumns);

            // 使用JdbcTemplate的批量操作
            int[][] results =
                    jdbcTemplate.batchUpdate(
                            updateSql,
                            dataList,
                            dataList.size(),
                            (ps, record) -> {
                                int paramIndex = 1;

                                // 设置非匹配字段的值
                                for (int i = 0; i < databaseFields.size(); i++) {
                                    String column = databaseFields.get(i);
                                    if (!upsertColumns.contains(column)) {
                                        Object value = record.get(i);
                                        ps.setObject(paramIndex, value);
                                        paramIndex++;
                                    }
                                }

                                // 设置WHERE条件的匹配字段值
                                for (String upsertColumn : upsertColumns) {
                                    int columnIndex = databaseFields.indexOf(upsertColumn);
                                    if (columnIndex >= 0 && columnIndex < record.size()) {
                                        Object columnValue = record.get(columnIndex);
                                        ps.setObject(paramIndex, columnValue);
                                        paramIndex++;
                                    }
                                }
                            });

            int successCount = 0;
            for (int[] result : results) {
                for (int updateCount : result) {
                    if (updateCount >= 0) {
                        successCount++;
                    }
                }
            }

            log.info(
                    "JdbcTemplate批量更新完成: 表={}, 成功={}, 总数={}",
                    tableName,
                    successCount,
                    dataList.size());
            return successCount;
        } catch (Exception e) {
            log.error("JdbcTemplate批量更新失败: 表={}, 错误={}", tableName, e.getMessage());
            throw new RuntimeException("批量更新失败: " + e.getMessage(), e);
        }
    }

    @Transactional
    @Override
    public int batchUpsert(
            String tableName,
            List<List<Object>> dataList,
            List<String> upsertColumns,
            List<String> databaseFields) {
        if (dataList == null || dataList.isEmpty()) {
            return 0;
        }

        if (upsertColumns == null || upsertColumns.isEmpty()) {
            throw new IllegalArgumentException("UPSERT模式必须指定冲突字段列表");
        }

        log.info(
                "开始JdbcTemplate批量UPSERT: 表={}, 数据量={}, 冲突字段={}",
                tableName,
                dataList.size(),
                upsertColumns);

        try {
            // 获取第一条记录来构建SQL
            String upsertSql = buildUpsertSql(tableName, databaseFields, upsertColumns);

            // 使用JdbcTemplate的批量操作
            int[][] results =
                    jdbcTemplate.batchUpdate(
                            upsertSql,
                            dataList,
                            dataList.size(),
                            (ps, record) -> {
                                for (int i = 0; i < databaseFields.size(); i++) {
                                    Object value = record.get(i);
                                    ps.setObject(i + 1, value);
                                }
                            });

            int successCount = 0;
            for (int[] result : results) {
                for (int updateCount : result) {
                    if (updateCount >= 0) {
                        successCount++;
                    }
                }
            }

            log.info(
                    "JdbcTemplate批量UPSERT完成: 表={}, 成功={}, 总数={}",
                    tableName,
                    successCount,
                    dataList.size());
            return successCount;
        } catch (Exception e) {
            log.error("JdbcTemplate批量UPSERT失败: 表={}, 错误={}", tableName, e.getMessage());
            throw new RuntimeException("批量UPSERT失败: " + e.getMessage(), e);
        }
    }

    @Transactional
    @Override
    public int executeBatchOperation(
            String tableName,
            List<List<Object>> dataList,
            ImportModeEnum importMode,
            String conflictStrategy,
            List<String> upsertColumns,
            List<String> databaseFields) {
        // 解析冲突策略
        ConflictStrategyEnum conflictStrategyEnum =
                conflictStrategy != null
                        ? ConflictStrategyEnum.getByCode(conflictStrategy)
                        : ConflictStrategyEnum.SKIP;
        if (conflictStrategyEnum == null) {
            log.warn("无效的冲突策略: {}, 使用默认策略 SKIP", conflictStrategy);
            conflictStrategyEnum = ConflictStrategyEnum.SKIP;
        }

        return switch (importMode) {
            case INSERT -> batchInsert(tableName, dataList, databaseFields, conflictStrategyEnum);
            case UPDATE -> {
                if (upsertColumns == null || upsertColumns.isEmpty()) {
                    throw new IllegalArgumentException("UPDATE模式必须指定匹配字段列表");
                }
                yield batchUpdate(tableName, dataList, upsertColumns, databaseFields);
            }
            case UPSERT -> {
                if (upsertColumns == null || upsertColumns.isEmpty()) {
                    throw new IllegalArgumentException("UPSERT模式必须指定冲突字段列表");
                }
                yield batchUpsert(tableName, dataList, upsertColumns, databaseFields);
            }
            default -> {
                log.warn("不支持的导入模式: {}, 使用默认的INSERT模式", importMode);
                yield batchInsert(tableName, dataList, databaseFields, conflictStrategyEnum);
            }
        };
    }

    private String buildInsertSql(String tableName, List<String> columns) {
        return buildInsertSqlWithConflictStrategy(tableName, columns, ConflictStrategyEnum.ERROR);
    }

    /**
     * 构建带冲突策略的INSERT SQL
     *
     * @param tableName 表名
     * @param columns 列名列表
     * @param conflictStrategy 冲突策略
     * @return SQL语句
     */
    private String buildInsertSqlWithConflictStrategy(
            String tableName, List<String> columns, ConflictStrategyEnum conflictStrategy) {
        StringBuilder sql = new StringBuilder("INSERT INTO ");
        sql.append(tableName).append(" (");

        boolean first = true;
        for (String column : columns) {
            if (!first) {
                sql.append(", ");
            }
            sql.append(column);
            first = false;
        }

        sql.append(") VALUES (");

        first = true;
        for (@SuppressWarnings("unused") String column : columns) {
            if (!first) {
                sql.append(", ");
            }
            sql.append("?");
            first = false;
        }

        sql.append(")");

        // 根据冲突策略添加 ON CONFLICT 子句
        if (conflictStrategy != null && columns.contains("id")) {
            switch (conflictStrategy) {
                case SKIP:
                    // 跳过冲突记录
                    sql.append(" ON CONFLICT (id) DO NOTHING");
                    break;
                case OVERWRITE:
                    // 覆盖冲突记录
                    sql.append(" ON CONFLICT (id) DO UPDATE SET ");
                    first = true;
                    for (String column : columns) {
                        if (!column.equals("id")) {
                            if (!first) {
                                sql.append(", ");
                            }
                            sql.append(column).append(" = EXCLUDED.").append(column);
                            first = false;
                        }
                    }
                    break;
                case ERROR:
                    // 遇到冲突时抛出错误（默认行为，不需要添加 ON CONFLICT）
                    break;
            }
        }

        return sql.toString();
    }

    private String buildUpdateSql(
            String tableName, List<String> columns, List<String> upsertColumns) {
        // 验证匹配字段是否都在列列表中
        for (String upsertColumn : upsertColumns) {
            if (!columns.contains(upsertColumn)) {
                throw new IllegalArgumentException(
                        String.format("匹配字段 %s 不在数据库字段列表中", upsertColumn));
            }
        }

        StringBuilder sql = new StringBuilder("UPDATE ");
        sql.append(tableName).append(" SET ");

        boolean first = true;
        for (String column : columns) {
            if (!upsertColumns.contains(column)) {
                if (!first) {
                    sql.append(", ");
                }
                sql.append(column).append(" = ?");
                first = false;
            }
        }

        sql.append(" WHERE ");
        first = true;
        for (String upsertColumn : upsertColumns) {
            if (!first) {
                sql.append(" AND ");
            }
            sql.append(upsertColumn).append(" = ?");
            first = false;
        }
        return sql.toString();
    }

    private String buildUpsertSql(
            String tableName, List<String> columns, List<String> upsertColumns) {
        // 验证冲突字段是否都在列列表中
        for (String upsertColumn : upsertColumns) {
            if (!columns.contains(upsertColumn)) {
                throw new IllegalArgumentException(
                        String.format("冲突字段 %s 不在数据库字段列表中", upsertColumn));
            }
        }

        StringBuilder sql = new StringBuilder("INSERT INTO ");
        sql.append(tableName).append(" (");

        boolean first = true;
        for (String column : columns) {
            if (!first) {
                sql.append(", ");
            }
            sql.append(column);
            first = false;
        }

        sql.append(") VALUES (");

        first = true;
        for (@SuppressWarnings("unused") String column : columns) {
            if (!first) {
                sql.append(", ");
            }
            sql.append("?");
            first = false;
        }

        sql.append(") ON CONFLICT (");
        first = true;
        for (String upsertColumn : upsertColumns) {
            if (!first) {
                sql.append(", ");
            }
            sql.append(upsertColumn);
            first = false;
        }
        sql.append(") DO UPDATE SET ");

        first = true;
        for (String column : columns) {
            if (!upsertColumns.contains(column)) {
                if (!first) {
                    sql.append(", ");
                }
                sql.append(column).append(" = EXCLUDED.").append(column);
                first = false;
            }
        }

        return sql.toString();
    }

    @Override
    public List<TableColumnInfo> getTableFields(String schema, String tableName) {
        log.info("开始获取表字段信息: 表={}", tableName);

        try {
            // PostgreSQL查询表字段信息的SQL，包括自增字段判断
            // 支持两种自增方式：
            // 1. SERIAL 类型（通过序列关联）
            // 2. IDENTITY 列（PostgreSQL 10+，通过 is_identity 或 attidentity 判断）
            // 使用 pg_catalog.format_type() 获取详细的类型信息，包括数组类型（如 integer[]、text[]）
            String sql =
                    """
                SELECT
                    isc.column_name,
                    isc.column_default,
                    isc.is_nullable,
                    -- 使用 pg_catalog.format_type() 获取完整的类型信息，包括数组类型
                    -- 例如：integer[]、text[]、bigint[] 等，而不是简单的 ARRAY
                    COALESCE(
                        pg_catalog.format_type(pa.atttypid, pa.atttypmod),
                        isc.data_type
                    ) as data_type,
                    isc.ordinal_position,
                    col_description(pgc.oid, isc.ordinal_position) as column_comment,
                    CASE
                        -- 检查 IDENTITY 列（PostgreSQL 10+）
                        WHEN isc.is_identity = 'YES' THEN true
                        -- 检查 attidentity（PostgreSQL 10+，'a' = always, 'd' = by default）
                        WHEN pa.attidentity IN ('a', 'd') THEN true
                        -- 检查传统 SERIAL 类型（通过序列关联）
                        WHEN seq.relname IS NOT NULL THEN true
                        ELSE false
                    END as is_auto_increment
                FROM information_schema.columns isc
                LEFT JOIN pg_class pgc ON pgc.relname = isc.table_name AND pgc.relnamespace = (
                    SELECT oid
                    FROM pg_namespace
                    WHERE nspname = isc.table_schema
                )
                LEFT JOIN pg_attribute pa ON pa.attrelid = pgc.oid
                    AND pa.attname = isc.column_name
                    AND pa.attnum > 0
                    AND NOT pa.attisdropped
                LEFT JOIN pg_attrdef ad ON ad.adrelid = pa.attrelid
                    AND ad.adnum = pa.attnum
                LEFT JOIN pg_depend dep ON dep.objid = ad.oid
                    AND dep.deptype = 'a'
                LEFT JOIN pg_class seq ON seq.oid = dep.refobjid
                    AND seq.relkind = 'S'
                WHERE isc.table_name = ?
                AND isc.table_schema = ?
                ORDER BY isc.ordinal_position
                """;

            List<TableColumnInfo> fields =
                    jdbcTemplate.query(sql, new TableColumnInfoRowMapper(), tableName, schema);

            log.info("获取表字段信息完成: 表={}, schema={}, 字段数量={}", tableName, schema, fields.size());
            return fields;

        } catch (Exception e) {
            log.error("获取表字段信息失败: 表={}, schema={}, 错误={}", tableName, schema, e.getMessage());
            throw new RuntimeException("获取表字段信息失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<String> getTableFieldNames(String tableName) {
        log.info("开始获取表字段名称: 表={}", tableName);

        try {
            String sql =
                    """
                SELECT column_name
                FROM information_schema.columns
                WHERE table_name = ?
                AND table_schema = 'public'
                ORDER BY ordinal_position
                """;

            List<String> fieldNames = jdbcTemplate.queryForList(sql, String.class, tableName);

            log.info("获取表字段名称完成: 表={}, 字段数量={}", tableName, fieldNames.size());
            return fieldNames;

        } catch (Exception e) {
            log.error("获取表字段名称失败: 表={}, 错误={}", tableName, e.getMessage());
            throw new RuntimeException("获取表字段名称失败: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean tableExists(String tableName) {
        log.debug("检查表是否存在: 表={}", tableName);

        try {
            String sql =
                    """
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_name = ?
                AND table_schema = 'public'
                """;

            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, tableName);
            boolean exists = count != null && count > 0;

            log.debug("表存在性检查完成: 表={}, 存在={}", tableName, exists);
            return exists;

        } catch (Exception e) {
            log.error("检查表是否存在失败: 表={}, 错误={}", tableName, e.getMessage());
            throw new RuntimeException("检查表是否存在失败: " + e.getMessage(), e);
        }
    }

    @Override
    public <T> T queryForObject(String sql, Class<T> elementType, Object... args) {
        log.debug("执行单值查询: sql={}, elementType={}", sql, elementType.getName());
        try {
            T result = jdbcTemplate.queryForObject(sql, elementType, args);
            log.debug("单值查询完成: 结果={}", result);
            return result;
        } catch (Exception e) {
            log.error("执行单值查询失败: sql={}, 错误={}", sql, e.getMessage());
            throw new RuntimeException("执行单值查询失败: " + e.getMessage(), e);
        }
    }

    @Override
    public <T> List<T> queryForList(String sql, Class<T> elementType, Object... args) {
        log.debug("执行查询: sql={}, elementType={}", sql, elementType.getName());
        try {
            List<T> results = jdbcTemplate.queryForList(sql, elementType, args);
            log.debug("查询完成: 结果数量={}", results.size());
            return results;
        } catch (Exception e) {
            log.error("执行查询失败: sql={}, 错误={}", sql, e.getMessage());
            throw new RuntimeException("执行查询失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Map<String, Object>> queryForMapList(String sql, Object... args) {
        log.debug("执行多列查询: sql={}", sql);
        try {
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, args);
            log.debug("查询完成: 结果数量={}", results.size());
            return results;
        } catch (Exception e) {
            log.error("执行多列查询失败: sql={}, 错误={}", sql, e.getMessage());
            throw new RuntimeException("执行多列查询失败: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, String> batchQueryByUniqueKeys(
            String tableName,
            List<String> uniqueKeyColumns,
            List<List<Object>> uniqueKeyValuesList,
            String targetField) {
        Map<String, String> resultMap = new HashMap<>();

        if (uniqueKeyValuesList == null || uniqueKeyValuesList.isEmpty()) {
            return resultMap;
        }

        if (uniqueKeyColumns == null || uniqueKeyColumns.isEmpty()) {
            log.warn("批量查询失败: 唯一键列为空");
            return resultMap;
        }

        try {
            // 构建批量查询SQL：使用多个OR条件
            StringBuilder sqlBuilder = new StringBuilder();
            sqlBuilder.append("SELECT ");

            // 添加唯一键字段和目标字段
            for (int i = 0; i < uniqueKeyColumns.size(); i++) {
                if (i > 0) {
                    sqlBuilder.append(", ");
                }
                sqlBuilder.append(uniqueKeyColumns.get(i));
            }
            sqlBuilder.append(", ").append(targetField);
            sqlBuilder.append(" FROM ").append(tableName).append(" WHERE ");

            // 构建OR条件
            List<Object> queryParams = new ArrayList<>();
            boolean firstCondition = true;
            for (List<Object> uniqueKeyValues : uniqueKeyValuesList) {
                if (uniqueKeyValues == null || uniqueKeyValues.size() != uniqueKeyColumns.size()) {
                    log.warn("批量查询跳过无效的唯一键值: {}", uniqueKeyValues);
                    continue;
                }

                if (!firstCondition) {
                    sqlBuilder.append(" OR ");
                }
                firstCondition = false;

                sqlBuilder.append("(");
                for (int i = 0; i < uniqueKeyColumns.size(); i++) {
                    if (i > 0) {
                        sqlBuilder.append(" AND ");
                    }
                    sqlBuilder.append(uniqueKeyColumns.get(i)).append(" = ?");
                    queryParams.add(uniqueKeyValues.get(i));
                }
                sqlBuilder.append(")");
            }

            // 如果没有有效的查询条件，直接返回
            if (queryParams.isEmpty()) {
                return resultMap;
            }

            String selectSql = sqlBuilder.toString();
            log.debug("执行批量查询: sql={}, 参数数量={}", selectSql, queryParams.size());

            // 执行批量查询
            List<Map<String, Object>> queryResults =
                    jdbcTemplate.queryForList(selectSql, queryParams.toArray());

            // 将查询结果转换为映射：唯一键字符串 -> 目标字段值
            for (Map<String, Object> row : queryResults) {
                // 构建唯一键字符串
                List<Object> keyValues = new ArrayList<>();
                for (String column : uniqueKeyColumns) {
                    keyValues.add(row.get(column));
                }
                String uniqueKey = buildUniqueKeyString(keyValues);

                // 获取目标字段值
                Object targetValue = row.get(targetField);
                if (targetValue != null) {
                    String targetValueStr = targetValue.toString();
                    resultMap.put(uniqueKey, targetValueStr);
                }
            }

            log.debug(
                    "批量查询完成: 表={}, 查询数量={}, 结果数量={}",
                    tableName,
                    uniqueKeyValuesList.size(),
                    resultMap.size());
        } catch (Exception e) {
            log.error("批量查询失败: 表={}, 错误={}", tableName, e.getMessage(), e);
            throw new RuntimeException("批量查询失败: " + e.getMessage(), e);
        }

        return resultMap;
    }

    /**
     * 构建唯一键的字符串表示，用于作为Map的key
     *
     * @param uniqueKeyValues 唯一键值列表
     * @return 唯一键字符串
     */
    private String buildUniqueKeyString(List<Object> uniqueKeyValues) {
        // 使用分隔符连接所有唯一键值，确保唯一性
        return uniqueKeyValues.stream()
                .map(value -> value != null ? value.toString() : "NULL")
                .collect(Collectors.joining("|||"));
    }

    @Override
    public int update(String sql, Object... args) {
        log.debug("执行更新: sql={}", sql);
        try {
            int affectedRows = jdbcTemplate.update(sql, args);
            log.debug("更新完成: 影响行数={}", affectedRows);
            return affectedRows;
        } catch (Exception e) {
            log.error("执行更新失败: sql={}, 错误={}", sql, e.getMessage());
            throw new RuntimeException("执行更新失败: " + e.getMessage(), e);
        }
    }

    @Override
    public long countRecords(String tableName) {
        log.debug("获取表记录数: 表={}", tableName);
        try {
            String sql = String.format("SELECT COUNT(*) FROM %s", tableName);
            Long count = jdbcTemplate.queryForObject(sql, Long.class);
            return count != null ? count : 0;
        } catch (Exception e) {
            log.error("获取表记录数失败: 表={}, 错误={}", tableName, e.getMessage());
            throw new RuntimeException("获取表记录数失败: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean recordExists(String tableName, String primaryKeyColumn, Object primaryKey) {
        log.debug("检查记录是否存在: 表={}, 主键列={}, 主键值={}", tableName, primaryKeyColumn, primaryKey);
        try {
            String sql =
                    String.format(
                            "SELECT COUNT(*) FROM %s WHERE %s = ?", tableName, primaryKeyColumn);
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, primaryKey);
            return count != null && count > 0;
        } catch (Exception e) {
            log.error(
                    "检查记录是否存在失败: 表={}, 主键列={}, 错误={}", tableName, primaryKeyColumn, e.getMessage());
            throw new RuntimeException("检查记录是否存在失败: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Object> queryByPrimaryKey(
            String tableName, String primaryKeyColumn, Object primaryKey) {
        log.debug("根据主键查询记录: 表={}, 主键列={}, 主键值={}", tableName, primaryKeyColumn, primaryKey);
        try {
            String sql =
                    String.format("SELECT * FROM %s WHERE %s = ?", tableName, primaryKeyColumn);
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, primaryKey);
            return results.isEmpty() ? null : results.get(0);
        } catch (Exception e) {
            log.error(
                    "根据主键查询记录失败: 表={}, 主键列={}, 错误={}", tableName, primaryKeyColumn, e.getMessage());
            throw new RuntimeException("根据主键查询记录失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Map<String, Object>> queryWithPagination(String tableName, int offset, int limit) {
        log.debug("分页查询记录: 表={}, offset={}, limit={}", tableName, offset, limit);
        try {
            String sql = String.format("SELECT * FROM %s LIMIT ? OFFSET ?", tableName);
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, limit, offset);
            log.debug("分页查询完成: 结果数量={}", results.size());
            return results;
        } catch (Exception e) {
            log.error("分页查询记录失败: 表={}, 错误={}", tableName, e.getMessage());
            throw new RuntimeException("分页查询记录失败: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public int insertRecord(String tableName, Map<String, Object> record) {
        log.debug("插入单条记录: 表={}", tableName);
        if (record == null || record.isEmpty()) {
            log.warn("记录为空，跳过插入");
            return 0;
        }

        try {
            // 构建INSERT语句
            StringBuilder sql = new StringBuilder("INSERT INTO ");
            sql.append(tableName);
            sql.append(" (");

            // 添加列名
            StringBuilder columns = new StringBuilder();
            StringBuilder placeholders = new StringBuilder();
            Object[] values = new Object[record.size()];
            int index = 0;

            for (Map.Entry<String, Object> entry : record.entrySet()) {
                if (index > 0) {
                    columns.append(", ");
                    placeholders.append(", ");
                }
                columns.append(entry.getKey());
                placeholders.append("?");
                values[index++] = entry.getValue();
            }

            sql.append(columns).append(") VALUES (").append(placeholders).append(")");

            int affectedRows = jdbcTemplate.update(sql.toString(), values);
            log.debug("插入记录完成: 影响行数={}", affectedRows);
            return affectedRows;
        } catch (Exception e) {
            log.error("插入单条记录失败: 表={}, 错误={}", tableName, e.getMessage());
            throw new RuntimeException("插入单条记录失败: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public int updateRecord(
            String tableName,
            Map<String, Object> record,
            String primaryKeyColumn,
            Object primaryKey) {
        log.debug("更新单条记录: 表={}, 主键列={}, 主键值={}", tableName, primaryKeyColumn, primaryKey);
        if (record == null || record.isEmpty()) {
            log.warn("记录为空，跳过更新");
            return 0;
        }

        try {
            // 构建UPDATE语句
            StringBuilder sql = new StringBuilder("UPDATE ");
            sql.append(tableName);
            sql.append(" SET ");

            // 添加SET子句
            StringBuilder setClause = new StringBuilder();
            int valueCount = 0;

            for (Map.Entry<String, Object> entry : record.entrySet()) {
                // 跳过主键列
                if (entry.getKey().equals(primaryKeyColumn)) {
                    continue;
                }

                if (valueCount > 0) {
                    setClause.append(", ");
                }
                setClause.append(entry.getKey()).append(" = ?");
                valueCount++;
            }

            sql.append(setClause);
            sql.append(" WHERE ").append(primaryKeyColumn).append(" = ?");

            // 准备参数
            Object[] values = new Object[valueCount + 1];
            int index = 0;

            for (Map.Entry<String, Object> entry : record.entrySet()) {
                if (!entry.getKey().equals(primaryKeyColumn)) {
                    values[index++] = entry.getValue();
                }
            }
            values[index] = primaryKey;

            int affectedRows = jdbcTemplate.update(sql.toString(), values);
            log.debug("更新记录完成: 影响行数={}", affectedRows);
            return affectedRows;
        } catch (Exception e) {
            log.error("更新单条记录失败: 表={}, 主键列={}, 错误={}", tableName, primaryKeyColumn, e.getMessage());
            throw new RuntimeException("更新单条记录失败: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public int updateField(
            String tableName,
            String fieldName,
            Object fieldValue,
            String primaryKeyColumn,
            Object primaryKey) {
        log.debug(
                "更新单个字段: 表={}, 字段={}, 主键列={}, 主键值={}",
                tableName,
                fieldName,
                primaryKeyColumn,
                primaryKey);
        try {
            String sql =
                    String.format(
                            "UPDATE %s SET %s = ? WHERE %s = ?",
                            tableName, fieldName, primaryKeyColumn);
            int affectedRows = jdbcTemplate.update(sql, fieldValue, primaryKey);
            log.debug("更新字段完成: 影响行数={}", affectedRows);
            return affectedRows;
        } catch (Exception e) {
            log.error("更新单个字段失败: 表={}, 字段={}, 错误={}", tableName, fieldName, e.getMessage());
            throw new RuntimeException("更新单个字段失败: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public int deleteRecord(String tableName, String primaryKeyColumn, Object primaryKey) {
        log.debug("删除单条记录: 表={}, 主键列={}, 主键值={}", tableName, primaryKeyColumn, primaryKey);
        try {
            String sql = String.format("DELETE FROM %s WHERE %s = ?", tableName, primaryKeyColumn);
            int affectedRows = jdbcTemplate.update(sql, primaryKey);
            log.debug("删除记录完成: 影响行数={}", affectedRows);
            return affectedRows;
        } catch (Exception e) {
            log.error("删除单条记录失败: 表={}, 主键列={}, 错误={}", tableName, primaryKeyColumn, e.getMessage());
            throw new RuntimeException("删除单条记录失败: " + e.getMessage(), e);
        }
    }

    @Override
    public javax.sql.DataSource getDataSource() {
        return jdbcTemplate.getDataSource();
    }

    @Override
    public boolean schemaExists(String schemaName) {
        log.debug("检查 schema 是否存在: {}", schemaName);
        try {
            String sql = "SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name = ?";
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, schemaName);
            return count != null && count > 0;
        } catch (Exception e) {
            log.error("检查 schema 是否存在失败: schema={}, 错误={}", schemaName, e.getMessage());
            throw new RuntimeException("检查 schema 是否存在失败: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean tableExists(String schemaName, String tableName) {
        log.debug("检查表是否存在: {}.{}", schemaName, tableName);
        try {
            String sql =
                    "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = ? AND table_name = ?";
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, schemaName, tableName);
            return count != null && count > 0;
        } catch (Exception e) {
            log.error(
                    "检查表是否存在失败: schema={}, table={}, 错误={}", schemaName, tableName, e.getMessage());
            throw new RuntimeException("检查表是否存在失败: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean columnExists(String schemaName, String tableName, String columnName) {
        log.debug("检查列是否存在: {}.{}.{}", schemaName, tableName, columnName);
        try {
            String sql =
                    "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = ? AND table_name = ? AND column_name = ?";
            Integer count =
                    jdbcTemplate.queryForObject(
                            sql, Integer.class, schemaName, tableName, columnName);
            return count != null && count > 0;
        } catch (Exception e) {
            log.error(
                    "检查列是否存在失败: schema={}, table={}, column={}, 错误={}",
                    schemaName,
                    tableName,
                    columnName,
                    e.getMessage());
            throw new RuntimeException("检查列是否存在失败: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean triggerExists(String schemaName, String tableName, String triggerName) {
        log.debug("检查触发器是否存在: {}.{}.{}", schemaName, tableName, triggerName);
        try {
            String sql =
                    "SELECT COUNT(*) FROM pg_trigger t "
                            + "JOIN pg_class c ON t.tgrelid = c.oid "
                            + "JOIN pg_namespace n ON c.relnamespace = n.oid "
                            + "WHERE n.nspname = ? AND c.relname = ? AND t.tgname = ?";
            Integer count =
                    jdbcTemplate.queryForObject(
                            sql, Integer.class, schemaName, tableName, triggerName);
            return count != null && count > 0;
        } catch (Exception e) {
            log.error(
                    "检查触发器是否存在失败: schema={}, table={}, trigger={}, 错误={}",
                    schemaName,
                    tableName,
                    triggerName,
                    e.getMessage());
            throw new RuntimeException("检查触发器是否存在失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void createNotificationTrigger(
            String schemaName, String tableName, String channel, String triggerName) {
        log.debug("创建通知触发器: schema={}, table={}, channel={}", schemaName, tableName, channel);
        try {
            String functionName = "notify_" + tableName.replace(".", "_");

            // 创建通知函数
            String createFunctionSql =
                    String.format(
                            """
            CREATE OR REPLACE FUNCTION %s.%s() RETURNS trigger AS $$
            DECLARE
              payload TEXT;
              pk_column TEXT;
              pk_value TEXT;
            BEGIN
              -- 获取主键列名
              SELECT a.attname INTO pk_column
              FROM pg_index i
              JOIN pg_attribute a ON a.attrelid = i.indrelid AND a.attnum = ANY(i.indkey)
              WHERE i.indrelid = '%s.%s'::regclass AND i.indisprimary
              LIMIT 1;
              IF (TG_OP = 'DELETE') THEN
                EXECUTE format('SELECT ($1).%%I::TEXT', pk_column) USING OLD INTO pk_value;
                payload := json_build_object('operation', TG_OP, 'table', TG_TABLE_NAME, 'primary_key', pk_value)::TEXT;
                PERFORM pg_notify('%s', payload);
                RETURN OLD;
              ELSE
                EXECUTE format('SELECT ($1).%%I::TEXT', pk_column) USING NEW INTO pk_value;
                payload := json_build_object('operation', TG_OP, 'table', TG_TABLE_NAME, 'primary_key', pk_value)::TEXT;
                PERFORM pg_notify('%s', payload);
                RETURN NEW;
              END IF;
            END;
            $$LANGUAGE plpgsql;
            """,
                            schemaName, functionName, schemaName, tableName, channel, channel);

            jdbcTemplate.execute(createFunctionSql);
            log.debug("创建通知函数完成: {}.{}", schemaName, functionName);

            // 创建触发器
            String createTriggerSql =
                    String.format(
                            "CREATE TRIGGER %s "
                                    + "AFTER INSERT OR UPDATE OR DELETE ON %s.%s "
                                    + "FOR EACH ROW EXECUTE FUNCTION %s.%s();",
                            triggerName, schemaName, tableName, schemaName, functionName);

            jdbcTemplate.execute(createTriggerSql);
            log.info("创建触发器完成: {} on table: {}.{}", triggerName, schemaName, tableName);

        } catch (Exception e) {
            log.error(
                    "创建通知触发器失败: schema={}, table={}, channel={}, 错误={}",
                    schemaName,
                    tableName,
                    channel,
                    e.getMessage());
            throw new RuntimeException("创建通知触发器失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void executeListen(String channel) {
        log.debug("执行 LISTEN: channel={}", channel);
        try {
            jdbcTemplate.execute("LISTEN " + channel);
        } catch (Exception e) {
            log.error("执行 LISTEN 失败: channel={}, 错误={}", channel, e.getMessage());
            throw new RuntimeException("执行 LISTEN 失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void executeUnlisten(String channel) {
        log.debug("执行 UNLISTEN: channel={}", channel);
        try {
            jdbcTemplate.execute("UNLISTEN " + channel);
        } catch (Exception e) {
            log.error("执行 UNLISTEN 失败: channel={}, 错误={}", channel, e.getMessage());
            throw new RuntimeException("执行 UNLISTEN 失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void executeUnlistenAll() {
        log.debug("执行 UNLISTEN *");
        try {
            jdbcTemplate.execute("UNLISTEN *");
        } catch (Exception e) {
            log.error("执行 UNLISTEN * 失败: 错误={}", e.getMessage());
            throw new RuntimeException("执行 UNLISTEN * 失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<String> getTablesBySchema(String schema) {
        log.info("开始获取 schema 下的所有表: schema={}", schema);
        try {
            String sql =
                    """
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = ?
                AND table_type = 'BASE TABLE'
                ORDER BY table_name
                """;

            List<String> tables = jdbcTemplate.queryForList(sql, String.class, schema);

            log.info("获取 schema 下的所有表完成: schema={}, 表数量={}", schema, tables.size());
            return tables;
        } catch (Exception e) {
            log.error("获取 schema 下的所有表失败: schema={}, 错误={}", schema, e.getMessage());
            throw new RuntimeException("获取 schema 下的所有表失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<TableInfo> getTablesWithCommentsBySchema(String schema) {
        log.info("开始获取 schema 下的所有表（含注释）: schema={}", schema);
        try {
            String sql =
                    """
                SELECT table_name, table_type, table_schema,
                       obj_description(('"' || table_schema || '"."' || table_name || '"')::regclass) AS table_comment
                FROM information_schema.tables
                WHERE table_schema = ? AND table_type = 'BASE TABLE'
                ORDER BY table_name
                """;

            List<TableInfo> tables =
                    jdbcTemplate.query(
                            sql,
                            (rs, rowNum) -> {
                                TableInfo info = new TableInfo();
                                info.setTableName(rs.getString("table_name"));
                                info.setTableType(rs.getString("table_type"));
                                info.setSchema(rs.getString("table_schema"));
                                info.setTableComment(rs.getString("table_comment"));
                                return info;
                            },
                            schema);

            log.info("获取 schema 下的所有表（含注释）完成: schema={}, 表数量={}", schema, tables.size());
            return tables;
        } catch (Exception e) {
            log.error("获取 schema 下的所有表（含注释）失败: schema={}, 错误={}", schema, e.getMessage());
            throw new RuntimeException("获取 schema 下的所有表失败: " + e.getMessage(), e);
        }
    }

    @Override
    public Object executeSql(String sql, boolean readOnly) {
        log.info(
                "执行SQL：{}",
                sql != null ? sql.substring(0, Math.min(sql.length(), 100)) + "..." : "null");

        if (sql == null || sql.trim().isEmpty()) {
            throw new IllegalArgumentException("SQL语句不能为空");
        }

        String sqlUpper = sql.trim().toUpperCase();
        if (sqlUpper.startsWith("SELECT")) {
            // 查询操作
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);
            log.info("SQL查询执行成功，返回 {} 条记录", results.size());
            return results;
        } else {
            // 更新操作（INSERT、UPDATE、DELETE等）
            if (readOnly) {
                throw new IllegalArgumentException("只读模式下不能执行更新操作");
            }
            int affectedRows = jdbcTemplate.update(sql);
            log.info("SQL更新执行成功，影响 {} 行", affectedRows);
            return affectedRows;
        }
    }

    @Override
    @Transactional
    public int deleteDataNotInUniqueKeyValues(
            String tableName,
            List<String> uniqueKeyColumns,
            java.util.Set<String> importUniqueKeyValues) {
        if (importUniqueKeyValues == null || importUniqueKeyValues.isEmpty()) {
            // 如果没有导入数据，清空整个表
            return truncateTable(tableName);
        }

        // 构建 WHERE 条件：唯一键值不在导入数据中
        // 使用 NOT IN 条件
        StringBuilder whereClause = new StringBuilder();
        if (uniqueKeyColumns.size() == 1) {
            // 单个字段：使用 field NOT IN (value1, value2, ...)
            String column = uniqueKeyColumns.get(0);
            List<String> valueList = new ArrayList<>();
            for (String uniqueKeyValue : importUniqueKeyValues) {
                String[] values = uniqueKeyValue.split("\\|\\|\\|");
                if (values.length > 0) {
                    String value = values[0];
                    if ("NULL".equals(value)) {
                        valueList.add("NULL");
                    } else {
                        String escapedValue = value.replace("'", "''");
                        valueList.add("'" + escapedValue + "'");
                    }
                }
            }
            if (valueList.isEmpty()) {
                return truncateTable(tableName);
            }
            whereClause
                    .append(column)
                    .append(" NOT IN (")
                    .append(String.join(", ", valueList))
                    .append(")");
        } else {
            // 多个字段：使用 (field1, field2, ...) NOT IN ((value1, value2, ...), ...)
            whereClause.append("(");
            whereClause.append(String.join(", ", uniqueKeyColumns));
            whereClause.append(") NOT IN (");

            List<String> valueTupleList = new ArrayList<>();
            for (String uniqueKeyValue : importUniqueKeyValues) {
                String[] values = uniqueKeyValue.split("\\|\\|\\|");
                if (values.length == uniqueKeyColumns.size()) {
                    StringBuilder valueTuple = new StringBuilder("(");
                    for (int i = 0; i < values.length; i++) {
                        if (i > 0) {
                            valueTuple.append(", ");
                        }
                        if ("NULL".equals(values[i])) {
                            valueTuple.append("NULL");
                        } else {
                            String escapedValue = values[i].replace("'", "''");
                            valueTuple.append("'").append(escapedValue).append("'");
                        }
                    }
                    valueTuple.append(")");
                    valueTupleList.add(valueTuple.toString());
                }
            }

            if (valueTupleList.isEmpty()) {
                return truncateTable(tableName);
            }

            whereClause.append(String.join(", ", valueTupleList));
            whereClause.append(")");
        }

        // 构建 DELETE SQL
        String deleteSql = String.format("DELETE FROM %s WHERE %s", tableName, whereClause);

        log.info("开始删除表中不在导入数据中的数据: 表={}, 导入数据记录数={}", tableName, importUniqueKeyValues.size());
        int affectedRows = update(deleteSql);
        log.info(
                "删除表中不在导入数据中的数据完成: 表={}, 删除记录数={}, 导入数据记录数={}",
                tableName,
                affectedRows,
                importUniqueKeyValues.size());
        return affectedRows;
    }

    @Override
    @Transactional
    public int truncateTable(String tableName) {
        String truncateSql = String.format("TRUNCATE TABLE %s", tableName);
        log.info("开始清空表: 表={}", tableName);
        int affectedRows = update(truncateSql);
        log.info("清空表完成: 表={}, 影响行数={}", tableName, affectedRows);
        return affectedRows;
    }

    @Override
    @Transactional
    public boolean createTempTableForUniqueKeys(
            String tempTableName, List<String> uniqueKeyColumns, List<String> columnTypes) {
        if (uniqueKeyColumns == null || uniqueKeyColumns.isEmpty()) {
            throw new IllegalArgumentException("唯一键列名列表不能为空");
        }
        if (columnTypes == null || columnTypes.size() != uniqueKeyColumns.size()) {
            throw new IllegalArgumentException("列类型列表必须与唯一键列名列表长度一致");
        }

        // 构建创建临时表的 SQL
        StringBuilder createSql = new StringBuilder("CREATE TEMP TABLE ");
        createSql.append(tempTableName).append(" (");

        for (int i = 0; i < uniqueKeyColumns.size(); i++) {
            if (i > 0) {
                createSql.append(", ");
            }
            createSql.append(uniqueKeyColumns.get(i)).append(" ").append(columnTypes.get(i));
        }

        // 如果是多列唯一键，创建复合唯一索引
        if (uniqueKeyColumns.size() > 1) {
            createSql.append(", UNIQUE (");
            createSql.append(String.join(", ", uniqueKeyColumns));
            createSql.append(")");
        } else {
            // 单列唯一键，创建唯一索引
            createSql.append(", UNIQUE (").append(uniqueKeyColumns.get(0)).append(")");
        }

        // 使用 ON COMMIT PRESERVE ROWS，确保临时表在整个会话期间都存在
        // 这样即使事务提交，临时表也不会被删除，直到会话结束
        createSql.append(") ON COMMIT PRESERVE ROWS");

        log.info("创建临时表: 表名={}, SQL={}", tempTableName, createSql);
        try {
            update(createSql.toString());
            return true;
        } catch (Exception e) {
            log.error("创建临时表失败: 表名={}, 错误={}", tempTableName, e.getMessage(), e);
            throw new RuntimeException("创建临时表失败: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public int batchInsertUniqueKeysToTempTable(
            String tempTableName,
            List<String> uniqueKeyColumns,
            List<List<Object>> uniqueKeyValuesList) {
        if (uniqueKeyValuesList == null || uniqueKeyValuesList.isEmpty()) {
            return 0;
        }

        // 构建 INSERT SQL
        String insertSql = buildInsertSql(tempTableName, uniqueKeyColumns);

        log.info("开始批量插入唯一键值到临时表: 临时表={}, 数据量={}", tempTableName, uniqueKeyValuesList.size());

        try {
            // 使用批量插入
            int[][] results =
                    jdbcTemplate.batchUpdate(
                            insertSql,
                            uniqueKeyValuesList,
                            uniqueKeyValuesList.size(),
                            (ps, record) -> {
                                for (int i = 0; i < uniqueKeyColumns.size(); i++) {
                                    Object value = i < record.size() ? record.get(i) : null;
                                    ps.setObject(i + 1, value);
                                }
                            });

            int successCount = 0;
            for (int[] result : results) {
                for (int updateCount : result) {
                    if (updateCount >= 0) {
                        successCount++;
                    }
                }
            }

            log.info(
                    "批量插入唯一键值到临时表完成: 临时表={}, 成功={}, 总数={}",
                    tempTableName,
                    successCount,
                    uniqueKeyValuesList.size());
            return successCount;
        } catch (Exception e) {
            log.error("批量插入唯一键值到临时表失败: 临时表={}, 错误={}", tempTableName, e.getMessage(), e);
            throw new RuntimeException("批量插入唯一键值到临时表失败: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public int deleteDataNotInTempTable(
            String tableName,
            String tempTableName,
            List<String> uniqueKeyColumns,
            List<List<String>> excludeKeyValues) {
        if (uniqueKeyColumns == null || uniqueKeyColumns.isEmpty()) {
            throw new IllegalArgumentException("唯一键列名列表不能为空");
        }

        // 构建 DELETE SQL，使用 LEFT JOIN 或 NOT EXISTS
        // 使用 NOT EXISTS 性能更好
        StringBuilder deleteSql = new StringBuilder("DELETE FROM ");
        deleteSql.append(tableName).append(" t WHERE NOT EXISTS (");
        deleteSql.append("SELECT 1 FROM ").append(tempTableName).append(" tmp WHERE ");

        // 构建 JOIN 条件
        for (int i = 0; i < uniqueKeyColumns.size(); i++) {
            if (i > 0) {
                deleteSql.append(" AND ");
            }
            String column = uniqueKeyColumns.get(i);
            deleteSql.append("t.").append(column).append(" = tmp.").append(column);
        }

        deleteSql.append(")");

        // 添加排除条件：如果配置了排除条件，则排除匹配的记录
        if (excludeKeyValues != null && !excludeKeyValues.isEmpty()) {
            deleteSql.append(" AND NOT (");
            for (int i = 0; i < excludeKeyValues.size(); i++) {
                if (i > 0) {
                    deleteSql.append(" OR ");
                }
                List<String> excludeValues = excludeKeyValues.get(i);
                deleteSql.append("(");
                for (int j = 0; j < uniqueKeyColumns.size(); j++) {
                    if (j > 0) {
                        deleteSql.append(" AND ");
                    }
                    String column = uniqueKeyColumns.get(j);
                    String excludeValue = j < excludeValues.size() ? excludeValues.get(j) : null;
                    if (excludeValue == null) {
                        deleteSql.append("t.").append(column).append(" IS NULL");
                    } else {
                        String escapedValue = excludeValue.replace("'", "''");
                        deleteSql
                                .append("t.")
                                .append(column)
                                .append(" = '")
                                .append(escapedValue)
                                .append("'");
                    }
                }
                deleteSql.append(")");
            }
            deleteSql.append(")");
        }

        int affectedRows = update(deleteSql.toString());
        log.info("使用临时表删除数据完成: 目标表={}, 临时表={}, 删除记录数={}", tableName, tempTableName, affectedRows);
        return affectedRows;
    }

    @Override
    @Transactional
    public void dropTempTable(String tempTableName) {
        String dropSql = String.format("DROP TABLE IF EXISTS %s", tempTableName);
        log.info("删除临时表: 表名={}", tempTableName);
        try {
            update(dropSql);
        } catch (Exception e) {
            log.warn("删除临时表失败: 表名={}, 错误={}", tempTableName, e.getMessage(), e);
            // 临时表在事务提交时会自动删除（ON COMMIT DROP），所以这里失败不影响
        }
    }

    private static class TableColumnInfoRowMapper implements RowMapper<TableColumnInfo> {
        @Override
        public TableColumnInfo mapRow(ResultSet rs, int rowNum) throws SQLException {
            TableColumnInfo info = new TableColumnInfo();
            info.setColumnName(rs.getString("column_name"));
            info.setColumnDefault(rs.getString("column_default"));
            // 将字符串 "YES"/"NO" 转换为布尔值
            String isNullableStr = rs.getString("is_nullable");
            info.setIsNullable("YES".equalsIgnoreCase(isNullableStr));
            info.setDataType(rs.getString("data_type"));
            info.setOrdinalPosition(rs.getInt("ordinal_position"));
            info.setColumnComment(rs.getString("column_comment"));
            // 设置自增字段标识
            Boolean isAutoIncrement = rs.getBoolean("is_auto_increment");
            if (rs.wasNull()) {
                info.setIsAutoIncrement(false);
            } else {
                info.setIsAutoIncrement(isAutoIncrement);
            }
            return info;
        }
    }
}
