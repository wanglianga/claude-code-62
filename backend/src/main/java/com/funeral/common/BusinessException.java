package com.funeral.common;

/** 业务异常：返回 400 与可读消息 */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
