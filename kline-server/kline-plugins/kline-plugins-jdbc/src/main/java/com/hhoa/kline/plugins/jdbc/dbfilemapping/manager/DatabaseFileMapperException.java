package com.hhoa.kline.plugins.jdbc.dbfilemapping.manager;

/** 数据库文件映射器异常 Exception thrown by DatabaseFileMapper operations */
public class DatabaseFileMapperException extends Exception {

    public DatabaseFileMapperException(String message) {
        super(message);
    }

    public DatabaseFileMapperException(String message, Throwable cause) {
        super(message, cause);
    }

    public DatabaseFileMapperException(Throwable cause) {
        super(cause);
    }
}
