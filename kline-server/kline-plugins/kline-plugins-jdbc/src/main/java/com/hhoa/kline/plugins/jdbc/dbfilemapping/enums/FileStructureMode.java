package com.hhoa.kline.plugins.jdbc.dbfilemapping.enums;

/** 文件结构模式枚举 File structure mode for database-file mapping */
public enum FileStructureMode {

    /**
     * 单JSON文件模式 每个记录对应一个JSON文件 文件路径：表名/主键.json 文件内容：{"field1": "value1", "field2": "value2", ...}
     * 不建议使用，因为如果数据量少，那么直接使用JDBC查询和修改更方便。
     *
     * <p>优点： - 文件数量少，监听开销小 - 适合字段较少的表 - 便于整体查看记录
     *
     * <p>缺点： - 修改单个字段需要重写整个文件 - 不适合大字段或字段很多的表
     */
    SINGLE_JSON,

    /**
     * 字段文件模式 每个字段对应一个独立文件 文件路径：表名/主键/字段名 文件内容：字段值（纯文本）
     *
     * <p>优点： - 可以单独修改某个字段 - 适合大字段的表
     *
     * <p>缺点： - 文件数量多，监听开销大 - 不适合字段较少的表
     */
    FIELD_FILES
}
