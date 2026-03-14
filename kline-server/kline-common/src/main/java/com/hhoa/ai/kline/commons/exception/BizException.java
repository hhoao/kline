package com.hhoa.ai.kline.commons.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class BizException extends RuntimeException {
    private String code;

    private String msg;

    public BizException(String msg) {
        super(msg);
        this.code = "500";
        this.msg = msg;
    }

    public BizException(String code, String msg) {
        super(msg);
        this.code = code;
        this.msg = msg;
    }

    public BizException(ErrorCode errorCode) {
        super(errorCode.getMsg());
        this.code = String.valueOf(errorCode.getCode());
        this.msg = errorCode.getMsg();
    }
}
