package com.hhoa.kline.plugins.jdbc.types;

import com.hhoa.ai.kline.commons.core.ArrayValuable;
import java.util.Arrays;
import java.util.Set;
import lombok.Getter;

/**
 * 数据库数据类型枚举 支持PostgreSQL、MySQL等主流数据库的数据类型
 *
 * @author hhoa
 * @date 2025/10/10
 */
@Getter
public enum DataTypeEnum implements ArrayValuable<String> {
    // 整数类型
    BIGINT("BIGINT", "大整数", Set.of("BIGINT", "INT8")),
    INTEGER("INTEGER", "整数", Set.of("INTEGER", "INT", "INT4")),
    SMALLINT("SMALLINT", "小整数", Set.of("SMALLINT", "INT2")),

    // 浮点数类型
    DECIMAL("DECIMAL", "精确小数", Set.of("DECIMAL", "NUMERIC")),
    REAL("REAL", "单精度浮点数", Set.of("REAL", "FLOAT4")),
    DOUBLE("DOUBLE", "双精度浮点数", Set.of("DOUBLE", "DOUBLE PRECISION", "FLOAT8")),
    FLOAT("FLOAT", "浮点数", Set.of("FLOAT")),

    // 字符串类型
    VARCHAR("VARCHAR", "可变长度字符串", Set.of("VARCHAR", "CHARACTER VARYING")),
    CHAR("CHAR", "固定长度字符串", Set.of("CHAR", "CHARACTER")),
    TEXT("TEXT", "长文本", Set.of("TEXT")),

    // 布尔类型
    BOOLEAN("BOOLEAN", "布尔值", Set.of("BOOLEAN", "BOOL")),

    // 日期时间类型
    DATE("DATE", "日期", Set.of("DATE")),
    TIME("TIME", "时间", Set.of("TIME", "TIME WITHOUT TIME ZONE")),
    TIMESTAMP("TIMESTAMP", "时间戳", Set.of("TIMESTAMP", "TIMESTAMP WITHOUT TIME ZONE")),
    TIMESTAMPTZ("TIMESTAMPTZ", "带时区的时间戳", Set.of("TIMESTAMP WITH TIME ZONE", "TIMESTAMPTZ")),

    // 其他类型
    UUID("UUID", "UUID", Set.of("UUID")),
    JSON("JSON", "JSON数据", Set.of("JSON")),
    JSONB("JSONB", "二进制JSON", Set.of("JSONB")),
    BYTEA("BYTEA", "二进制数据", Set.of("BYTEA")),
    INET("INET", "IP地址", Set.of("INET")),
    CIDR("CIDR", "网络地址", Set.of("CIDR")),
    MACADDR("MACADDR", "MAC地址", Set.of("MACADDR")),
    ARRAY("ARRAY", "数组", Set.of("ARRAY")),

    // 未知类型
    UNKNOWN("UNKNOWN", "未知类型", Set.of());

    public static final String[] ARRAYS =
            Arrays.stream(values()).map(DataTypeEnum::getStandardName).toArray(String[]::new);

    /** 标准类型名称 */
    private final String standardName;

    /** 类型描述 */
    private final String description;

    /** 支持的数据库类型名称集合 */
    private final Set<String> supportedNames;

    DataTypeEnum(String standardName, String description, Set<String> supportedNames) {
        this.standardName = standardName;
        this.description = description;
        this.supportedNames = supportedNames;
    }

    @Override
    public String[] array() {
        return ARRAYS;
    }

    /**
     * 根据数据库类型名称获取标准数据类型枚举
     *
     * @param dbTypeName 数据库类型名称
     * @return 标准数据类型枚举，未找到时返回UNKNOWN
     */
    public static DataTypeEnum fromDbTypeName(String dbTypeName) {
        if (dbTypeName == null || dbTypeName.trim().isEmpty()) {
            return UNKNOWN;
        }

        String normalizedName = dbTypeName.trim().toUpperCase();

        for (DataTypeEnum dataType : values()) {
            if (dataType.getSupportedNames().contains(normalizedName)) {
                return dataType;
            }
        }

        return UNKNOWN;
    }

    /**
     * 根据标准类型名称获取数据类型枚举
     *
     * @param standardName 标准类型名称
     * @return 数据类型枚举，未找到时返回UNKNOWN
     */
    public static DataTypeEnum fromStandardName(String standardName) {
        if (standardName == null || standardName.trim().isEmpty()) {
            return UNKNOWN;
        }

        String normalizedName = standardName.trim().toUpperCase();

        for (DataTypeEnum dataType : values()) {
            if (dataType.getStandardName().equals(normalizedName)) {
                return dataType;
            }
        }

        return UNKNOWN;
    }

    /**
     * 检查是否为数值类型
     *
     * @return 是否为数值类型
     */
    public boolean isNumeric() {
        return this == BIGINT
                || this == INTEGER
                || this == SMALLINT
                || this == DECIMAL
                || this == REAL
                || this == DOUBLE
                || this == FLOAT;
    }

    /**
     * 检查是否为整数类型
     *
     * @return 是否为整数类型
     */
    public boolean isInteger() {
        return this == BIGINT || this == INTEGER || this == SMALLINT;
    }

    /**
     * 检查是否为浮点数类型
     *
     * @return 是否为浮点数类型
     */
    public boolean isFloatingPoint() {
        return this == DECIMAL || this == REAL || this == DOUBLE || this == FLOAT;
    }

    /**
     * 检查是否为字符串类型
     *
     * @return 是否为字符串类型
     */
    public boolean isString() {
        return this == VARCHAR || this == CHAR || this == TEXT;
    }

    /**
     * 检查是否为日期时间类型
     *
     * @return 是否为日期时间类型
     */
    public boolean isDateTime() {
        return this == DATE || this == TIME || this == TIMESTAMP || this == TIMESTAMPTZ;
    }

    /**
     * 检查是否为布尔类型
     *
     * @return 是否为布尔类型
     */
    public boolean isBoolean() {
        return this == BOOLEAN;
    }

    /**
     * 检查是否为JSON类型
     *
     * @return 是否为JSON类型
     */
    public boolean isJson() {
        return this == JSON || this == JSONB;
    }

    /**
     * 验证类型名称是否有效
     *
     * @param typeName 类型名称
     * @return 是否有效
     */
    public static boolean isValidType(String typeName) {
        return fromDbTypeName(typeName) != UNKNOWN;
    }
}
