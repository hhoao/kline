package com.hhoa.kline.plugins.jdbc.dbfilemapping.watcher;

import com.hhoa.kline.plugins.jdbc.dbfilemapping.model.MappingConfiguration;
import java.nio.file.Path;

/** 文件监听器接口 Interface for monitoring file system changes */
public interface FileWatcher {

    /**
     * 注册目录监听 Register a directory for monitoring
     *
     * @param directory 要监听的目录
     * @param config 映射配置
     * @throws FileWatcherException 如果注册失败
     */
    void registerDirectory(Path directory, MappingConfiguration config) throws FileWatcherException;

    /**
     * 取消目录监听 Unregister a directory from monitoring
     *
     * @param directory 要取消监听的目录
     */
    void unregisterDirectory(Path directory);

    /**
     * 启动文件监听 Start the file watcher
     *
     * @throws FileWatcherException 如果启动失败
     */
    void start() throws FileWatcherException;

    /** 停止文件监听 Stop the file watcher */
    void stop();

    /**
     * 设置文件变更回调 Set the callback for file change events
     *
     * @param callback 回调接口
     */
    void setFileChangeCallback(FileChangeCallback callback);

    /**
     * 检查监听器是否正在运行 Check if the watcher is running
     *
     * @return true if running, false otherwise
     */
    boolean isRunning();
}
