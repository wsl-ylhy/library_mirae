package com.mirae.library.common.error;

/**
 * 重复归还异常(409)。
 */
public final class LoanAlreadyReturnedException extends BusinessException {

    public LoanAlreadyReturnedException(Long loanId) {
        super(ErrorCode.LOAN_ALREADY_RETURNED, "Loan with id " + loanId + " has already been returned");
    }
}
