package com.hhoa.kline.plugins.jdbc.dbfilemapping.enums;

/** 字段值格式枚举，仅适用于 FIELD_FILES 模式 */
public enum FieldValueFormat {

    /** JSON 格式化：将字段值作为 JSON 解析并进行美化输出。 DB→文件时 pretty-print，格式化后同步回 DB。 */
    JSON
}
