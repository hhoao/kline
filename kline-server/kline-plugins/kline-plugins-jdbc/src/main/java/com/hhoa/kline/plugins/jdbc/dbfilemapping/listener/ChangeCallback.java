package com.hhoa.kline.plugins.jdbc.dbfilemapping.listener;

/** 数据库变更回调接口 Callback interface for database change notifications */
public interface ChangeCallback {

    /**
     * 当记录插入时调用 Called when a record is inserted
     *
     * @param tableName 表名
     * @param primaryKey 主键值
     */
    void onInsert(String tableName, Object primaryKey);

    /**
     * 当记录更新时调用 Called when a record is updated
     *
     * @param tableName 表名
     * @param primaryKey 主键值
     */
    void onUpdate(String tableName, Object primaryKey);

    /**
     * 当记录删除时调用 Called when a record is deleted
     *
     * @param tableName 表名
     * @param primaryKey 主键值
     */
    void onDelete(String tableName, Object primaryKey);
}
