package com.mirae.library.loan;

import com.mirae.library.book.Book;
import com.mirae.library.member.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 借阅领域实体。
 * 不变式:同一会员对同一本书至多一条未归还借阅(服务层保证);
 * 每笔借阅只能归还一次;归还时间不能早于借出时间。
 */
@Entity
@Table(name = "loans")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Loan {

    /** 借阅期限(天),应还日期 = 借出日期 + 14 天 可做成配置*/
    public static final int LOAN_PERIOD_DAYS = 14;

    /** 主键,数据库自增 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 所借书籍(懒加载,列表查询时通过 @EntityGraph 预取) */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    /** 借阅人(懒加载) */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    /** 借出时间 */
    @Column(nullable = false)
    private LocalDateTime borrowedAt;

    /** 应还日期 */
    @Column(nullable = false)
    private LocalDate dueDate;

    /** 实际归还时间,null 表示尚未归还 */
    @Column
    private LocalDateTime returnedAt;

    private Loan(Book book, Member member, LocalDateTime borrowedAt) {
        this.book = book;
        this.member = member;
        this.borrowedAt = borrowedAt;
        //应还日期由借出时间自动推导
        this.dueDate = borrowedAt.toLocalDate().plusDays(LOAN_PERIOD_DAYS);
    }

    /**
     * 静态工厂:创建一条新的借阅记录。
     */
    public static Loan create(Book book, Member member, LocalDateTime borrowedAt) {
        return new Loan(book, member, borrowedAt);
    }

    /**
     * 是否仍在借阅中(未归还)。
     */
    public boolean isActive() {
        return returnedAt == null;
    }

    /**
     * 标记归还。只能对未归还的借阅调用一次。
     */
    public void markReturned(LocalDateTime returnedAt) {
        if (!isActive()) {
            throw new IllegalStateException("Loan " + id + " is already returned"); // 重复归还
        }
        if (returnedAt.isBefore(borrowedAt)) {
            throw new IllegalStateException("returnedAt cannot be before borrowedAt"); // 归还早于借出
        }
        this.returnedAt = returnedAt;
    }
}
