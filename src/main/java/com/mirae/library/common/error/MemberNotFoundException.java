package com.mirae.library.common.error;

/**
 * 会员不存在异常(404)。
 */
public final class MemberNotFoundException extends BusinessException {

    public MemberNotFoundException(Long memberId) {
        super(ErrorCode.MEMBER_NOT_FOUND, "Member with id " + memberId + " not found");
    }
}
