package com.mirae.library.common.error;

/**
 * 请求参数非法异常(400)。用于无法用 Bean Validation 覆盖的场景,如查询参数取值非法。
 */
public final class InvalidRequestException extends BusinessException {

    public InvalidRequestException(String message) {
        super(ErrorCode.INVALID_REQUEST, message);
    }
}
