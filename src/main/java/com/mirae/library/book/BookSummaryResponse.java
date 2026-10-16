package com.mirae.library.book;

/**
 * 书籍列表项 DTO(浏览接口的响应元素)。
 */
public record BookSummaryResponse(Long id, String title, String author, int availableCopies) {

    /**
     * 从实体转换为列表项。
     */
    public static BookSummaryResponse from(Book book) {
        return new BookSummaryResponse(book.getId(), book.getTitle(), book.getAuthor(), book.getAvailableCopies());
    }
}
