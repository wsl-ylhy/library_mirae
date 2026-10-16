package com.mirae.library.loan;

import static org.assertj.core.api.Assertions.assertThat;

import com.mirae.library.book.Book;
import com.mirae.library.book.BookRepository;
import com.mirae.library.common.error.BookNotAvailableException;
import com.mirae.library.common.error.ErrorCode;
import com.mirae.library.config.DataSeeder;
import com.mirae.library.member.MemberRepository;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 借阅并发集成测试:两名会员同时抢借仅剩的一本副本,
 * 验证 BookRepository 的原子条件 UPDATE 保证恰好一人成功、库存不为负。
 */
@SpringBootTest
class BorrowConcurrencyTest {

    @Autowired
    private LoanService loanService;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private DataSeeder dataSeeder;

    /** 种子数据中仅剩 1 本可借的书 */
    private Long singleCopyBookId;

    /**
     * 每个用例前重置数据,并解析仅剩 1 本可借的书。
     */
    @BeforeEach
    void resetData() {
        loanRepository.deleteAll();
        bookRepository.deleteAll();
        memberRepository.deleteAll();
        dataSeeder.seed();

        singleCopyBookId = bookRepository.findByAuthorContainingIgnoreCase("Hunt").get(0).getId();
    }

    /**
     * 并发核心用例:两个线程同时借最后一本,结果必须是一成功、一 409,且库存归零。
     */
    @Test
    void onlyOneOfTwoConcurrentBorrowsOfLastCopySucceeds() throws Exception {
        List<Long> memberIds = memberRepository.findAll().stream().map(m -> m.getId()).toList();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startGate = new CountDownLatch(1); // 起跑闸门,让两个线程尽量同时发起借阅

        try {
            Future<ErrorCode> first = executor.submit(() -> attemptBorrow(startGate, memberIds.get(0)));
            Future<ErrorCode> second = executor.submit(() -> attemptBorrow(startGate, memberIds.get(1)));
            startGate.countDown(); // 同时放行

            // 恰好一个成功(返回 null)、一个失败(BOOK_NOT_AVAILABLE)
            ErrorCode firstOutcome = first.get(10, TimeUnit.SECONDS);
            ErrorCode secondOutcome = second.get(10, TimeUnit.SECONDS);
            assertThat(new ErrorCode[]{firstOutcome, secondOutcome})
                    .containsExactlyInAnyOrder(null, ErrorCode.BOOK_NOT_AVAILABLE);
        } finally {
            executor.shutdownNow(); // 无论结果如何都回收线程池
        }

        Book book = bookRepository.findById(singleCopyBookId).orElseThrow();
        assertThat(book.getAvailableCopies()).isZero(); // 库存不为负
        assertThat(loanRepository.findByMemberIdAndReturnedAtIsNull(memberIds.get(0)).size()
                + loanRepository.findByMemberIdAndReturnedAtIsNull(memberIds.get(1)).size()).isEqualTo(1); // 仅产生 1 条借阅
    }

    /**
     * 辅助方法:等待起跑闸门后发起一次借阅。
     * 成功返回 null;预期的"无可用副本"失败返回对应错误码。
     */
    private ErrorCode attemptBorrow(CountDownLatch startGate, Long memberId) throws Exception {
        startGate.await();
        try {
            loanService.borrow(memberId, singleCopyBookId);
            return null;
        } catch (BookNotAvailableException e) {
            return e.getErrorCode();
        }
    }
}
