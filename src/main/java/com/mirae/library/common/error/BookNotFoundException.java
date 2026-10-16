package com.mirae.library.common.error;

/**
 * 书籍不存在异常(404)。
 */
public final class BookNotFoundException extends BusinessException {

    public BookNotFoundException(Long bookId) {
        super(ErrorCode.BOOK_NOT_FOUND, "Book with id " + bookId + " not found");
    }
}
