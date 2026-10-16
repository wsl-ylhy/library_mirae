package com.mirae.library.common.error;

/**
 * 业务异常基类:所有可预期的业务失败都以 BusinessException 子类抛出。
 * 每个子类绑定一个 ErrorCode,由全局异常处理器统一转换为 HTTP 响应。
 */
public abstract class BusinessException extends RuntimeException {

    /** 对应的错误码(决定 HTTP 状态码与响应体 code 字段) */
    private final ErrorCode errorCode;

    protected BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
