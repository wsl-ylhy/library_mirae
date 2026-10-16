package com.mirae.library.common.error;

import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * 全局异常处理器:把所有异常统一转换为 ApiError 结构的 HTTP 响应,
 * 保证业务异常、参数校验失败、未知异常三条路径的响应体格式一致。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 业务异常:按异常携带的 ErrorCode 映射 HTTP 状态码。
     */
    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ApiError> handleBusiness(BusinessException ex, HttpServletRequest request) {
        ErrorCode code = ex.getErrorCode();
        return ResponseEntity.status(code.getStatus())
                .body(new ApiError(code, ex.getMessage(), request.getRequestURI()));
    }

    /**
     * 请求体参数校验失败(@Valid):400,并附带逐字段错误信息。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(fieldError.getField(),
                    Objects.requireNonNullElse(fieldError.getDefaultMessage(), "invalid"));
        }
        return ResponseEntity.badRequest().body(
                new ApiError(ErrorCode.INVALID_REQUEST, "Request validation failed", request.getRequestURI(), fieldErrors));
    }

    /**
     * 请求体 JSON 无法解析 / 路径变量类型不匹配:400。
     */
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<ApiError> handleMalformedRequest(Exception ex, HttpServletRequest request) {
        return ResponseEntity.badRequest().body(
                new ApiError(ErrorCode.INVALID_REQUEST, "Malformed request", request.getRequestURI()));
    }

    /**
     * 兜底:未预期异常 → 500。记录完整堆栈到日志,但不向客户端泄露内部细节。
     */
    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error while handling {}", request.getRequestURI(), ex);
        return ResponseEntity.internalServerError().body(
                new ApiError(ErrorCode.INTERNAL_ERROR, "An unexpected error occurred", request.getRequestURI()));
    }
}
