package com.mirae.library.loan;

import com.mirae.library.book.BookRepository;
import com.mirae.library.common.error.BookNotAvailableException;
import com.mirae.library.common.error.BookNotFoundException;
import com.mirae.library.common.error.DuplicateActiveLoanException;
import com.mirae.library.common.error.LoanAlreadyReturnedException;
import com.mirae.library.common.error.LoanNotFoundException;
import com.mirae.library.member.MemberRepository;
import com.mirae.library.member.MemberService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 借阅用例服务:借书、还书、查询借阅列表。写操作在事务内完成。
 */
@Service
public class LoanService {

    private final LoanRepository loanRepository;
    private final BookRepository bookRepository;
    private final MemberRepository memberRepository;
    private final MemberService memberService;

    public LoanService(LoanRepository loanRepository, BookRepository bookRepository,
                       MemberRepository memberRepository, MemberService memberService) {
        this.loanRepository = loanRepository;
        this.bookRepository = bookRepository;
        this.memberRepository = memberRepository;
        this.memberService = memberService;
    }

    /**
     * 借书流程(事务):
     * 1. 校验会员与书籍存在
     * 2. 校验同一本书没有未归还的借阅
     * 3. 原子扣减库存(失败即无可借副本)
     * 4. 创建借阅记录
     */
    @Transactional
    public LoanResponse borrow(Long memberId, Long bookId) {
        memberService.requireExists(memberId); // 会员不存在 → 404
        if (!bookRepository.existsById(bookId)) {
            throw new BookNotFoundException(bookId); // 书籍不存在 → 404
        }
        if (loanRepository.existsByMemberIdAndBookIdAndReturnedAtIsNull(memberId, bookId)) {
            throw new DuplicateActiveLoanException(memberId, bookId); // 重复借阅 → 409
        }
        // 原子条件扣减:并发抢借最后一本时,数据库行锁保证只有一人扣减成功
        if (bookRepository.decrementIfAvailable(bookId) == 0) {
            throw new BookNotAvailableException(bookId); // 无可用副本 → 409
        }
        // 存在性已校验,用 getReferenceById 获取懒加载代理,省去多余的 SELECT
        Loan loan = Loan.create(
                bookRepository.getReferenceById(bookId),
                memberRepository.getReferenceById(memberId),
                LocalDateTime.now());
        return LoanResponse.from(loanRepository.save(loan));
    }

    /**
     * 归还流程(事务):
     * 1. 查询借阅记录(不存在 → 404)
     * 2. 校验尚未归还(重复归还 → 409)
     * 3. 原子恢复库存
     * 4. 记录归还时间
     */
    @Transactional
    public LoanResponse returnBook(Long loanId) {
        Loan loan = loanRepository.findWithDetailsById(loanId)
                .orElseThrow(() -> new LoanNotFoundException(loanId)); // 借阅不存在 → 404
        if (!loan.isActive()) {
            throw new LoanAlreadyReturnedException(loanId); // 重复归还 → 409
        }
        bookRepository.incrementIfNotFull(loan.getBook().getId()); // 恢复库存(上限保护)
        loan.markReturned(LocalDateTime.now()); // 记录归还时间,事务提交时落库
        return LoanResponse.from(loan);
    }

    /**
     * 查询某会员的借阅列表,按状态区分当前借阅/历史记录。会员不存在 → 404。
     */
    @Transactional(readOnly = true)
    public List<LoanResponse> loansOfMember(Long memberId, LoanStatus status) {
        memberService.requireExists(memberId); // 会员不存在 → 404
        List<Loan> loans = switch (status) {
            case ACTIVE -> loanRepository.findByMemberIdAndReturnedAtIsNull(memberId);    // 未归还的当前借阅
            case RETURNED -> loanRepository.findByMemberIdAndReturnedAtIsNotNull(memberId); // 已归还的历史记录
        };
        return loans.stream().map(LoanResponse::from).toList();
    }
}
