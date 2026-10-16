package com.mirae.library.common.error;

import org.springframework.http.HttpStatus;

/**
 * 业务错误码枚举:集中定义错误码与 HTTP 状态码的映射。
 * 新增错误只需在此增加一项,全局异常处理器会自动按此映射响应。
 */
public enum ErrorCode {

    BOOK_NOT_FOUND(HttpStatus.NOT_FOUND),           // 书籍不存在 → 404
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND),         // 会员不存在 → 404
    LOAN_NOT_FOUND(HttpStatus.NOT_FOUND),           // 借阅记录不存在 → 404
    BOOK_NOT_AVAILABLE(HttpStatus.CONFLICT),        // 无可用副本 → 409
    DUPLICATE_ACTIVE_LOAN(HttpStatus.CONFLICT),     // 重复借阅同一本书 → 409
    LOAN_ALREADY_RETURNED(HttpStatus.CONFLICT),     // 重复归还 → 409
    INVALID_REQUEST(HttpStatus.BAD_REQUEST),        // 请求参数非法 → 400
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR); // 未预期错误 → 500

    /** 该错误码对应的 HTTP 状态码 */
    private final HttpStatus status;

    ErrorCode(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
