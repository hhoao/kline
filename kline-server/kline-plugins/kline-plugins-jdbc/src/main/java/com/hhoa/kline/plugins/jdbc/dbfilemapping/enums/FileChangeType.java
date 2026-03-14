package com.hhoa.kline.plugins.jdbc.dbfilemapping.enums;

/** 文件变更类型枚举 Represents the type of file system change detected */
public enum FileChangeType {
    /** 文件创建 - File was created */
    CREATED,

    /** 文件修改 - File was modified */
    MODIFIED,

    /** 文件删除 - File was deleted */
    DELETED
}
