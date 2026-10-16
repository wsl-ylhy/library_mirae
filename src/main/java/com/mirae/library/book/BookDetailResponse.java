package com.mirae.library.book;

/**
 * 书籍详情 DTO(详情接口的响应体)。
 */
public record BookDetailResponse(
        Long id, String isbn, String title, String author, int totalCopies, int availableCopies) {

    /**
     * 从实体转换为详情。
     */
    public static BookDetailResponse from(Book book) {
        return new BookDetailResponse(book.getId(), book.getIsbn(), book.getTitle(), book.getAuthor(),
                book.getTotalCopies(), book.getAvailableCopies());
    }
}
