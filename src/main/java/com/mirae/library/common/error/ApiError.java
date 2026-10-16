package com.mirae.library.common.error;

import java.time.Instant;
import java.util.Map;

/**
 * 统一的错误响应体:所有 4xx/5xx 响应结构一致,便于客户端统一解析。
 */
public record ApiError(
        ErrorCode code,
        String message,
        String path,
        Instant timestamp,
        Map<String, String> fieldErrors) {

    /**
     * 无逐字段错误的便捷构造。
     */
    public ApiError(ErrorCode code, String message, String path) {
        this(code, message, path, Instant.now(), null);
    }

    /**
     * 带逐字段错误的便捷构造(参数校验失败时使用)。
     */
    public ApiError(ErrorCode code, String message, String path, Map<String, String> fieldErrors) {
        this(code, message, path, Instant.now(), fieldErrors);
    }
}
