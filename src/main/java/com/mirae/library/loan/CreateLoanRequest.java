package com.mirae.library.loan;

import jakarta.validation.constraints.NotNull;

/**
 * 借书请求体。字段缺失时由 Bean Validation 拦截,返回 400 与逐字段错误信息。
 */
public record CreateLoanRequest(
        @NotNull(message = "bookId is required") Long bookId,
        @NotNull(message = "memberId is required") Long memberId) {
}
