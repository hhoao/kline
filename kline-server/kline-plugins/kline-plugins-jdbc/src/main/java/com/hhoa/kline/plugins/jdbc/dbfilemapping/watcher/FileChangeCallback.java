package com.hhoa.kline.plugins.jdbc.dbfilemapping.watcher;

import java.nio.file.Path;

/** 文件变更回调接口 Callback interface for file system change events */
public interface FileChangeCallback {

    /**
     * 文件创建时的回调 Called when a file is created
     *
     * @param filePath 创建的文件路径
     */
    void onFileCreated(Path filePath);

    /**
     * 文件修改时的回调 Called when a file is modified
     *
     * @param filePath 修改的文件路径
     */
    void onFileModified(Path filePath);

    /**
     * 文件删除时的回调 Called when a file is deleted
     *
     * @param filePath 删除的文件路径
     */
    void onFileDeleted(Path filePath);
}
