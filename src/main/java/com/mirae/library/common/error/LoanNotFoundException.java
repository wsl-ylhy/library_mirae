package com.mirae.library.common.error;

/**
 * 借阅记录不存在异常(404)。
 */
public final class LoanNotFoundException extends BusinessException {

    public LoanNotFoundException(Long loanId) {
        super(ErrorCode.LOAN_NOT_FOUND, "Loan with id " + loanId + " not found");
    }
}
