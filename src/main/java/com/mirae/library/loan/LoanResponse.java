package com.mirae.library.loan;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 借阅响应体(借书/归还/列表接口共用)。
 */
public record LoanResponse(
        Long id,
        Long bookId,
        String bookTitle,
        Long memberId,
        String memberName,
        LocalDateTime borrowedAt,
        LocalDate dueDate,
        LocalDateTime returnedAt) {

    /**
     * 从实体转换。涉及 book/member 懒加载关联,必须在事务内调用。
     */
    public static LoanResponse from(Loan loan) {
        return new LoanResponse(
                loan.getId(),
                loan.getBook().getId(),
                loan.getBook().getTitle(),
                loan.getMember().getId(),
                loan.getMember().getName(),
                loan.getBorrowedAt(),
                loan.getDueDate(),
                loan.getReturnedAt());
    }
}
