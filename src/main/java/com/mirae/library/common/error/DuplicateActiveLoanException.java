package com.mirae.library.common.error;

/**
 * 同一会员重复借阅同一本书异常(409)。
 */
public final class DuplicateActiveLoanException extends BusinessException {

    public DuplicateActiveLoanException(Long memberId, Long bookId) {
        super(ErrorCode.DUPLICATE_ACTIVE_LOAN,
                "Member " + memberId + " already has an active loan for book " + bookId);
    }
}
