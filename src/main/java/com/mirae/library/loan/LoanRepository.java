package com.mirae.library.loan;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 借阅仓储。
 * 列表与详情查询通过 @EntityGraph 预取 book/member 关联,避免懒加载代理在事务外失效及 N+1 查询。
 */
public interface LoanRepository extends JpaRepository<Loan, Long> {

    /** 判断某会员对某本书是否已有未归还的借阅(借书时的重复校验) */
    boolean existsByMemberIdAndBookIdAndReturnedAtIsNull(Long memberId, Long bookId);

    /** 某会员的当前借阅列表(未归还) */
    @EntityGraph(attributePaths = {"book", "member"})
    List<Loan> findByMemberIdAndReturnedAtIsNull(Long memberId);

    /** 某会员的历史借阅列表(已归还) */
    @EntityGraph(attributePaths = {"book", "member"})
    List<Loan> findByMemberIdAndReturnedAtIsNotNull(Long memberId);

    /** 按 id 查询并预取书籍与会员(归还时使用) */
    @EntityGraph(attributePaths = {"book", "member"})
    Optional<Loan> findWithDetailsById(Long id);
}
