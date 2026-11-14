package com.campus.secondhand.exception;

/**
 * 业务异常：所有可预期的业务失败都抛这个，由全局异常处理器统一转成 R 或错误页
 */
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int code;

    public BusinessException(String message) {
        super(message);
        this.code = 500;
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}