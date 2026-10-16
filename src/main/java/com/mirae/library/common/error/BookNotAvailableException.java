package com.mirae.library.common.error;

/**
 * 书籍无可用副本异常(409)。
 */
public final class BookNotAvailableException extends BusinessException {

    public BookNotAvailableException(Long bookId) {
        super(ErrorCode.BOOK_NOT_AVAILABLE, "Book with id " + bookId + " has no available copies");
    }
}
