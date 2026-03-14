package com.hhoa.kline.plugins.jdbc.dbfilemapping.enums;

/** 错误类型枚举 Categorizes different types of errors that can occur during synchronization */
public enum ErrorType {
    /** 文件读取错误 - Error reading file */
    FILE_READ_ERROR,

    /** 文件写入错误 - Error writing file */
    FILE_WRITE_ERROR,

    /** JSON解析错误 - Error parsing JSON */
    JSON_PARSE_ERROR,

    /** 数据库错误 - Database operation error */
    DATABASE_ERROR,

    /** 验证错误 - Data validation error */
    VALIDATION_ERROR,

    /** 冲突错误 - Conflict resolution error */
    CONFLICT_ERROR
}
