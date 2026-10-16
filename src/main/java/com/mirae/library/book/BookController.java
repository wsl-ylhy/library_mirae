package com.mirae.library.book;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 书籍 REST 接口。
 */
@RestController
@RequestMapping("/api/books")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    /**
     * GET /api/books —— 浏览书籍列表,支持 author / available 两个可选过滤参数。
     */
    @GetMapping
    public List<BookSummaryResponse> browse(@RequestParam(required = false) String author,
                                            @RequestParam(required = false) Boolean available) {
        return bookService.browse(author, available).stream()
                .map(BookSummaryResponse::from)
                .toList();
    }

    /**
     * GET /api/books/{id} —— 书籍详情,不存在时返回 404。
     */
    @GetMapping("/{id}")
    public BookDetailResponse getById(@PathVariable Long id) {
        return BookDetailResponse.from(bookService.getById(id));
    }
}
