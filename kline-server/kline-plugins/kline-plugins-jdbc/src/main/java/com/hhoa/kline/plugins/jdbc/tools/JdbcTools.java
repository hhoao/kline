package com.hhoa.kline.plugins.jdbc.tools;

import com.hhoa.kline.core.common.tool.common.CommonTool;
import com.hhoa.kline.plugins.jdbc.core.SchemaContextHolder;
import com.hhoa.kline.plugins.jdbc.service.JdbcService;
import com.hhoa.kline.plugins.jdbc.service.TableColumnInfo;
import com.hhoa.kline.plugins.jdbc.tools.dto.JdbcExecuteSqlDTO;
import com.hhoa.kline.plugins.jdbc.tools.dto.JdbcGetColumnsDTO;
import com.hhoa.kline.web.common.pojo.CommonResult;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

/**
 * Hasura客户端工具类 提供数据库表查询、字段获取、SQL执行等功能
 *
 * @author xianxing
 * @since 2025/9/23
 */
@Slf4j
@Component
@AllArgsConstructor
public class JdbcTools implements CommonTool {
    private final JdbcService jdbcService;

    @Tool(description = "获取所有表列表")
    public CommonResult<List<String>> getTables() {
        String schema = SchemaContextHolder.getSchema();
        List<String> tableNames = jdbcService.getTablesBySchema(schema);

        log.info("成功获取Schema [{}] 下的 {} 个表", schema, tableNames.size());
        return CommonResult.success(
                tableNames, "成功获取Schema [" + schema + "] 下的 " + tableNames.size() + " 个表");
    }

    @Tool(description = "获取指定表的字段列表")
    public CommonResult<List<TableColumnInfo>> getColumns(JdbcGetColumnsDTO request) {
        String schema = SchemaContextHolder.getSchema();
        List<TableColumnInfo> columns = jdbcService.getTableFields(schema, request.getTableName());

        return CommonResult.success(
                columns, "成功获取表 [" + request.getTableName() + "] 的 " + columns.size() + " 个字段");
    }

    @Tool(description = "执行SQL语句")
    public CommonResult<Object> executeSql(JdbcExecuteSqlDTO request) {
        log.info(
                "执行SQL：{}",
                request.getSql() != null
                        ? request.getSql().substring(0, Math.min(request.getSql().length(), 100))
                                + "..."
                        : "null");

        // 设置默认值
        boolean readOnly = request.getReadOnly() != null ? request.getReadOnly() : true;

        // 使用 JdbcService 执行 SQL
        Object sqlResult = jdbcService.executeSql(request.getSql(), readOnly);

        return CommonResult.success(sqlResult);
    }
}
