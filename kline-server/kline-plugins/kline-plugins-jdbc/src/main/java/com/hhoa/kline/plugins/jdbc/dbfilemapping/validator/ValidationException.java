package com.hhoa.kline.plugins.jdbc.dbfilemapping.validator;

/** 验证异常 Exception thrown when validation fails */
public class ValidationException extends Exception {

    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
