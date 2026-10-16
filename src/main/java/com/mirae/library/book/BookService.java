package com.mirae.library.book;

import com.mirae.library.common.error.BookNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 书籍查询服务(只读)。
 */
@Service
@Transactional(readOnly = true)
public class BookService {

    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    /**
     * 浏览书籍,支持两个可选过滤条件:
     * author    —— 作者部分匹配(忽略大小写)
     * available —— 为 true 时仅返回有可借副本的书;false/null 不过滤
     */
    public List<Book> browse(String author, Boolean available) {
        boolean hasAuthor = author != null && !author.isBlank();
        if (hasAuthor && Boolean.TRUE.equals(available)) {
            // 两个条件同时提供:组合过滤
            return bookRepository.findByAuthorContainingIgnoreCaseAndAvailableCopiesGreaterThan(author.trim(), 0);
        }
        if (hasAuthor) {
            // 仅按作者过滤
            return bookRepository.findByAuthorContainingIgnoreCase(author.trim());
        }
        if (Boolean.TRUE.equals(available)) {
            // 仅过滤可借状态
            return bookRepository.findByAvailableCopiesGreaterThan(0);
        }
        // 无条件:返回全部书籍
        return bookRepository.findAll();
    }

    /**
     * 查询书籍详情,不存在时抛 BookNotFoundException(映射为 404)。
     */
    public Book getById(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new BookNotFoundException(id));
    }
}
