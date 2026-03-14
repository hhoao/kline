package com.hhoa.kline.plugins.jdbc.dbfilemapping.engine;

/** 同步异常 Exception thrown when synchronization operations fail */
public class SyncException extends Exception {

    public SyncException(String message) {
        super(message);
    }

    public SyncException(String message, Throwable cause) {
        super(message, cause);
    }
}
