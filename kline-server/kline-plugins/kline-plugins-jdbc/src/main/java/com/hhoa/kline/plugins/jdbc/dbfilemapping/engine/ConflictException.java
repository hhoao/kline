package com.hhoa.kline.plugins.jdbc.dbfilemapping.engine;

/** 冲突异常 Exception thrown when a conflict is detected during synchronization */
public class ConflictException extends Exception {

    public ConflictException(String message) {
        super(message);
    }

    public ConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
