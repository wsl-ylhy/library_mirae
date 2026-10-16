package com.mirae.library.book;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mirae.library.config.DataSeeder;
import com.mirae.library.loan.LoanRepository;
import com.mirae.library.member.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 书籍接口集成测试:覆盖浏览列表、过滤、详情与 404 场景。
 * 每个用例前清空三张表并重新写入种子数据,保证用例相互独立。
 */
@SpringBootTest
@AutoConfigureMockMvc
class BookApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private DataSeeder dataSeeder;

    /**
     * 每个用例前重置数据:先删借阅(外键依赖),再删书籍与会员,最后重跑种子数据。
     */
    @BeforeEach
    void resetData() {
        loanRepository.deleteAll();
        bookRepository.deleteAll();
        memberRepository.deleteAll();
        dataSeeder.seed();
    }

    /**
     * 浏览:无过滤条件时返回全部 4 本种子书。
     */
    @Test
    void browseReturnsAllSeededBooks() throws Exception {
        mockMvc.perform(get("/api/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[0].title").exists())
                .andExpect(jsonPath("$[0].author").exists())
                .andExpect(jsonPath("$[0].availableCopies").isNumber());
    }

    /**
     * 浏览:按作者部分匹配(忽略大小写)过滤。
     */
    @Test
    void browseFiltersByAuthor() throws Exception {
        mockMvc.perform(get("/api/books").param("author", "eric"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Domain-Driven Design"));
    }

    /**
     * 浏览:available=true 时仅返回有可借副本的书(种子数据中 1 本已借完)。
     */
    @Test
    void browseFiltersByAvailability() throws Exception {
        mockMvc.perform(get("/api/books").param("available", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));
    }

    /**
     * 详情:返回 ISBN、总册数等完整信息。
     */
    @Test
    void getBookDetailReturnsFullInfo() throws Exception {
        Long bookId = bookRepository.findAll().get(0).getId();

        mockMvc.perform(get("/api/books/{id}", bookId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(bookId))
                .andExpect(jsonPath("$.isbn").exists())
                .andExpect(jsonPath("$.totalCopies").isNumber())
                .andExpect(jsonPath("$.availableCopies").isNumber());
    }

    /**
     * 详情:书籍不存在时返回 404 与统一错误体。
     */
    @Test
    void getBookDetailReturns404WithUnifiedErrorBody() throws Exception {
        mockMvc.perform(get("/api/books/{id}", 99999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("BOOK_NOT_FOUND"))
                .andExpect(jsonPath("$.path").value("/api/books/99999"));
    }
}
