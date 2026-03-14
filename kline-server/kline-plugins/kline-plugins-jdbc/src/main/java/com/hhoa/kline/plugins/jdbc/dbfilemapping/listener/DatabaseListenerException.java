package com.hhoa.kline.plugins.jdbc.dbfilemapping.listener;

/** 数据库监听器异常 Exception thrown by database listener operations */
public class DatabaseListenerException extends Exception {

    public DatabaseListenerException(String message) {
        super(message);
    }

    public DatabaseListenerException(String message, Throwable cause) {
        super(message, cause);
    }

    public DatabaseListenerException(Throwable cause) {
        super(cause);
    }
}
