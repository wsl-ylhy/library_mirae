package com.mirae.library.book;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

/**
 * 书籍仓储。查询方法由 Spring Data JPA 按方法名自动生成。
 */
public interface BookRepository extends JpaRepository<Book, Long> {

    /** 按作者模糊查询(忽略大小写) */
    List<Book> findByAuthorContainingIgnoreCase(String author);

    /** 查询至少还有 1 本可借的书 */
    List<Book> findByAvailableCopiesGreaterThan(int min);

    /** 组合查询:作者模糊匹配 + 有可借副本 */
    List<Book> findByAuthorContainingIgnoreCaseAndAvailableCopiesGreaterThan(String author, int min);

    /**
     * 原子扣减库存:检查与扣减在同一条 SQL 中完成,并发抢借最后一本时不会超卖。
     * 返回受影响行数,0 表示没有可借副本。
     */
    @Modifying
    @Query("update Book b set b.availableCopies = b.availableCopies - 1 where b.id = :id and b.availableCopies > 0")
    int decrementIfAvailable(Long id);

    /**
     * 原子恢复库存:上限条件保证 availableCopies <= totalCopies 的不变式不被破坏。
     */
    @Modifying
    @Query("update Book b set b.availableCopies = b.availableCopies + 1 where b.id = :id and b.availableCopies < b.totalCopies")
    int incrementIfNotFull(Long id);
}
