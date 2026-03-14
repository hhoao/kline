package com.hhoa.kline.plugins.jdbc.dbfilemapping.listener;

/** 数据库变更监听器接口 Interface for listening to PostgreSQL database changes using LISTEN/NOTIFY */
public interface DatabaseChangeListener {

    /**
     * 监听指定表的变更 Listen to changes on a specific table
     *
     * @param tableName 表名
     * @param callback 变更回调
     * @throws DatabaseListenerException 监听失败时抛出
     */
    void listenToTable(String tableName, ChangeCallback callback) throws DatabaseListenerException;

    /**
     * 停止监听指定表 Stop listening to a specific table
     *
     * @param tableName 表名
     */
    void stopListening(String tableName);

    /**
     * 启动监听器 Start the listener
     *
     * @throws DatabaseListenerException 启动失败时抛出
     */
    void start() throws DatabaseListenerException;

    /** 停止监听器并清理资源 Stop the listener and cleanup resources */
    void stop();

    /**
     * 检查监听器是否正在运行 Check if the listener is running
     *
     * @return true if running, false otherwise
     */
    boolean isRunning();
}
