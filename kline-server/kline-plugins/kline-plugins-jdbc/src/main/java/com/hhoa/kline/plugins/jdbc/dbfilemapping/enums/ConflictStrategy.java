package com.hhoa.kline.plugins.jdbc.dbfilemapping.enums;

/** 冲突解决策略枚举 Defines strategies for resolving conflicts when both file and database are modified */
public enum ConflictStrategy {
    /**
     * 自动选择策略 - Automatically select best strategy based on table structure 优先级: VERSION_WINS >
     * UPDATE_TIME_WINS > TIMESTAMP_WINS > FILE_WINS
     *
     * <p>检测逻辑: 1. 如果表有version字段 -> VERSION_WINS 2. 如果表有update_time/updated_at字段 -> UPDATE_TIME_WINS
     * 3. 如果表有created_at字段 -> TIMESTAMP_WINS 4. 否则 -> FILE_WINS
     */
    AUTO,

    /** 文件优先 - File content takes precedence over database */
    FILE_WINS,

    /** 数据库优先 - Database content takes precedence over file */
    DATABASE_WINS,

    /** 时间戳优先 - Most recent modification wins based on timestamp */
    TIMESTAMP_WINS,

    /**
     * 版本号优先 - Version field determines the winner (optimistic locking) 使用version字段进行乐观锁控制，版本号大的获胜
     */
    VERSION_WINS,

    /** 更新时间优先 - update_time field determines the winner 使用update_time字段判断，时间更新的获胜 */
    UPDATE_TIME_WINS,

    /**
     * 拉取后更新 - Always fetch latest before update (pessimistic approach) 更新前总是先拉取最新版本，确保基于最新数据进行更新
     */
    FETCH_BEFORE_UPDATE
}
