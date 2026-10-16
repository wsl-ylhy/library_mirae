package com.mirae.library.loan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mirae.library.book.Book;
import com.mirae.library.book.BookRepository;
import com.mirae.library.config.DataSeeder;
import com.mirae.library.member.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * 借阅接口集成测试:覆盖借书、归还、借阅列表的全部正常与异常场景。
 * 每个用例前重置数据,种子数据含一本无可借副本的书(用于 409 场景)。
 */
@SpringBootTest
@AutoConfigureMockMvc
class LoanApiTest {

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

    /** 用例内缓存的资源 id,由种子数据解析而来 */
    private Long memberId;
    private Long otherMemberId;
    private Long availableBookId;
    private Long unavailableBookId;

    /**
     * 每个用例前重置数据并解析测试所需的书籍/会员 id(不硬编码,因为 H2 自增序列不会随 deleteAll 重置)。
     */
    @BeforeEach
    void resetData() {
        loanRepository.deleteAll();
        bookRepository.deleteAll();
        memberRepository.deleteAll();
        dataSeeder.seed();

        memberId = memberRepository.findAll().get(0).getId();
        otherMemberId = memberRepository.findAll().get(1).getId();
        availableBookId = bookRepository.findByAuthorContainingIgnoreCase("Evans").get(0).getId();
        unavailableBookId = bookRepository.findByAuthorContainingIgnoreCase("Kleppmann").get(0).getId();
    }

    /**
     * 借书成功:201 + Location 头 + 借阅信息,且库存减 1。
     */
    @Test
    void borrowCreatesLoanAndDecrementsInventory() throws Exception {
        int before = availableCopies(availableBookId);

        MvcResult result = mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"bookId": %d, "memberId": %d}
                                """.formatted(availableBookId, memberId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bookId").value(availableBookId))
                .andExpect(jsonPath("$.memberId").value(memberId))
                .andExpect(jsonPath("$.borrowedAt").exists())
                .andExpect(jsonPath("$.dueDate").exists())
                .andExpect(jsonPath("$.returnedAt").doesNotExist())
                .andReturn();

        long loanId = loanIdFrom(result);
        assertThat(result.getResponse().getHeader("Location")).isEqualTo("/api/loans/" + loanId); // Location 指向新资源
        assertThat(result.getResponse().getContentAsString()).contains("\"returnedAt\":null");
        assertThat(availableCopies(availableBookId)).isEqualTo(before - 1); // 库存减 1
    }

    /**
     * 借无可借副本的书:409 BOOK_NOT_AVAILABLE。
     */
    @Test
    void borrowUnavailableBookReturns409() throws Exception {
        mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"bookId": %d, "memberId": %d}
                                """.formatted(unavailableBookId, memberId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BOOK_NOT_AVAILABLE"));
    }

    /**
     * 同一会员重复借同一本书:409 DUPLICATE_ACTIVE_LOAN。
     */
    @Test
    void borrowSameBookTwiceReturns409() throws Exception {
        borrow(memberId, availableBookId); // 先正常借出

        mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"bookId": %d, "memberId": %d}
                                """.formatted(availableBookId, memberId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_ACTIVE_LOAN"));
    }

    /**
     * 借不存在的书:404 BOOK_NOT_FOUND。
     */
    @Test
    void borrowUnknownBookReturns404() throws Exception {
        mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"bookId": 99999, "memberId": %d}
                                """.formatted(memberId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("BOOK_NOT_FOUND"));
    }

    /**
     * 不存在的会员借书:404 MEMBER_NOT_FOUND。
     */
    @Test
    void borrowForUnknownMemberReturns404() throws Exception {
        mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"bookId": %d, "memberId": 99999}
                                """.formatted(availableBookId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MEMBER_NOT_FOUND"));
    }

    /**
     * 请求体缺字段:400 INVALID_REQUEST,并带逐字段错误信息。
     */
    @Test
    void borrowWithMissingFieldReturns400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"memberId": %d}
                                """.formatted(memberId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.fieldErrors.bookId").value("bookId is required"));
    }

    /**
     * 请求体不是合法 JSON:400 INVALID_REQUEST。
     */
    @Test
    void borrowWithMalformedJsonReturns400() throws Exception {
        mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not valid json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    /**
     * 归还成功:返回归还时间,且库存恢复 1 本。
     */
    @Test
    void returnBookRestoresInventoryAndSetsReturnedAt() throws Exception {
        long loanId = borrow(memberId, availableBookId);
        int before = availableCopies(availableBookId);

        mockMvc.perform(post("/api/loans/{loanId}/return", loanId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.returnedAt").exists());

        assertThat(availableCopies(availableBookId)).isEqualTo(before + 1); // 库存恢复
    }

    /**
     * 重复归还同一笔借阅:409 LOAN_ALREADY_RETURNED。
     */
    @Test
    void returnSameLoanTwiceReturns409() throws Exception {
        long loanId = borrow(memberId, availableBookId);
        mockMvc.perform(post("/api/loans/{loanId}/return", loanId)).andExpect(status().isOk()); // 第一次归还成功

        mockMvc.perform(post("/api/loans/{loanId}/return", loanId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("LOAN_ALREADY_RETURNED"));
    }

    /**
     * 归还不存在的借阅记录:404 LOAN_NOT_FOUND。
     */
    @Test
    void returnUnknownLoanReturns404() throws Exception {
        mockMvc.perform(post("/api/loans/{loanId}/return", 99999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("LOAN_NOT_FOUND"));
    }

    /**
     * 当前借阅列表:只显示该会员未归还的借阅,其他会员的借阅不可见。
     */
    @Test
    void activeLoansListShowsOnlyCurrentLoans() throws Exception {
        borrow(memberId, availableBookId);

        mockMvc.perform(get("/api/members/{memberId}/loans", memberId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].bookId").value(availableBookId))
                .andExpect(jsonPath("$[0].returnedAt").doesNotExist());

        mockMvc.perform(get("/api/members/{memberId}/loans", otherMemberId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    /**
     * 归还后:默认列表(当前借阅)为空,status=returned 显示历史记录。
     */
    @Test
    void returnedLoansListShowsHistoryAfterReturn() throws Exception {
        long loanId = borrow(memberId, availableBookId);
        mockMvc.perform(post("/api/loans/{loanId}/return", loanId)).andExpect(status().isOk());

        mockMvc.perform(get("/api/members/{memberId}/loans", memberId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0))); // 归还后无当前借阅

        mockMvc.perform(get("/api/members/{memberId}/loans", memberId).param("status", "returned"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].returnedAt").exists());
    }

    /**
     * 查询不存在会员的借阅列表:404 MEMBER_NOT_FOUND。
     */
    @Test
    void loansOfUnknownMemberReturns404() throws Exception {
        mockMvc.perform(get("/api/members/{memberId}/loans", 99999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MEMBER_NOT_FOUND"));
    }

    /**
     * status 参数取值非法:400 INVALID_REQUEST。
     */
    @Test
    void loansWithInvalidStatusReturns400() throws Exception {
        mockMvc.perform(get("/api/members/{memberId}/loans", memberId).param("status", "expired"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    /**
     * 辅助方法:执行一次成功的借书请求,返回新借阅的 id。
     */
    private long borrow(Long memberId, Long bookId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"bookId": %d, "memberId": %d}
                                """.formatted(bookId, memberId)))
                .andExpect(status().isCreated())
                .andReturn();
        return loanIdFrom(result);
    }

    /**
     * 辅助方法:从响应 JSON 中解析借阅 id。
     */
    private static long loanIdFrom(MvcResult result) throws Exception {
        return Long.parseLong(result.getResponse().getContentAsString().replaceAll(".*\"id\":(\\d+).*", "$1"));
    }

    /**
     * 辅助方法:读取书籍当前可借册数。
     */
    private int availableCopies(Long bookId) {
        Book book = bookRepository.findById(bookId).orElseThrow();
        return book.getAvailableCopies();
    }
}
