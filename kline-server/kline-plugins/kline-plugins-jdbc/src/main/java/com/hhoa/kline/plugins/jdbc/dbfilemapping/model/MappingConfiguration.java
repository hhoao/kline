package com.hhoa.kline.plugins.jdbc.dbfilemapping.model;

import com.hhoa.kline.plugins.jdbc.dbfilemapping.enums.ConflictStrategy;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.enums.FieldValueFormat;
import com.hhoa.kline.plugins.jdbc.dbfilemapping.enums.FileStructureMode;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/** 映射配置类 Configuration for mapping a database table to a file directory */
@Data
@NoArgsConstructor
@EqualsAndHashCode(of = {"schemaName", "tableName"})
public class MappingConfiguration {

    /** Schema名称 (默认public) */
    private String schemaName = "public";

    /** 表名 */
    private String tableName;

    /** 目标目录 */
    private String targetDirectory;

    /** 主键列名 */
    private String primaryKeyColumn;

    /** 包含字段 (null=全部) */
    private List<String> includedFields;

    /** 排除字段 */
    private List<String> excludedFields;

    /** 字段名称映射 (数据库字段名 -> JSON字段名) */
    private Map<String, String> fieldNameMapping;

    /** 字段值转换函数 (字段名 -> 转换函数) */
    private Map<String, Function<Object, Object>> fieldValueTransformers;

    /** 冲突策略 */
    private ConflictStrategy conflictStrategy = ConflictStrategy.TIMESTAMP_WINS;

    /** 版本字段名称 (用于VERSION_WINS策略) 例如: "version", "row_version", "revision" */
    private String versionColumn;

    /** 更新时间字段名称 (用于UPDATE_TIME_WINS策略) 例如: "update_time", "updated_at", "modified_at" */
    private String updateTimeColumn;

    /** 创建时间字段名称 (用于时间戳比较的后备选项) 例如: "create_time", "created_at" */
    private String createTimeColumn;

    /** 启用实时同步 */
    private boolean enableRealTimeSync = true;

    /** 防抖延迟(毫秒) 默认500ms */
    private int debounceMillis = 500;

    /** 批处理大小 默认100 */
    private int maxBatchSize = 100;

    /** 同步间隔(秒) 默认5秒 */
    private int syncIntervalSeconds = 5;

    /** 文件结构模式 - SINGLE_JSON: 每个记录一个JSON文件 (表名/主键.json) - FIELD_FILES: 每个字段一个文件 (表名/主键/字段名) */
    private FileStructureMode fileStructureMode = FileStructureMode.SINGLE_JSON;

    /** 字段值格式 (字段名 -> 格式类型)，仅 FIELD_FILES 模式有效。 DB→文件同步时，对指定字段按格式类型格式化，格式化后的值同步回 DB。 */
    private Map<String, FieldValueFormat> fieldValueFormats;

    /** 字段值格式化开关 (字段名 -> 是否启用格式化)，仅 FIELD_FILES 模式有效 */
    private Map<String, Boolean> fieldValueFormatEnabled;

    public MappingConfiguration(String tableName, String targetDirectory, String primaryKeyColumn) {
        this.tableName = tableName;
        this.targetDirectory = targetDirectory;
        this.primaryKeyColumn = primaryKeyColumn;
    }

    /** 获取完全限定的表名 (schema.table) */
    public String getQualifiedTableName() {
        return schemaName + "." + tableName;
    }
}
