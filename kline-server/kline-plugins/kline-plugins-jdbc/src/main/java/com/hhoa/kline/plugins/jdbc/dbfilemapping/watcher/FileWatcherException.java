package com.hhoa.kline.plugins.jdbc.dbfilemapping.watcher;

/** 文件监听器异常 Exception thrown by FileWatcher operations */
public class FileWatcherException extends Exception {

    public FileWatcherException(String message) {
        super(message);
    }

    public FileWatcherException(String message, Throwable cause) {
        super(message, cause);
    }

    public FileWatcherException(Throwable cause) {
        super(cause);
    }
}
