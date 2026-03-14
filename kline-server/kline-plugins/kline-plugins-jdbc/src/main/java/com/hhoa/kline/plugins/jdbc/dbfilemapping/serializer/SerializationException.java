package com.hhoa.kline.plugins.jdbc.dbfilemapping.serializer;

/** 序列化异常 Exception thrown when serialization or deserialization fails */
public class SerializationException extends Exception {

    public SerializationException(String message) {
        super(message);
    }

    public SerializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
