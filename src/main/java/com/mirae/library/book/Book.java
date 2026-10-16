package com.mirae.library.book;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 书籍领域实体。
 * 不变式:0 <= availableCopies <= totalCopies,库存变更只能通过 borrow()/returnCopy() 完成。
 */
@Entity
@Table(name = "books")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 需要无参构造;设为 protected 防止外部绕过构造器创建非法对象
public class Book {

    /** 主键,数据库自增 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 国际标准书号,全局唯一 */
    @Column(nullable = false, unique = true)
    private String isbn;

    /** 书名 */
    @Column(nullable = false)
    private String title;

    /** 作者 */
    @Column(nullable = false)
    private String author;

    /** 馆藏总册数 */
    @Column(nullable = false)
    private int totalCopies;

    /** 当前可借册数 */
    @Column(nullable = false)
    private int availableCopies;

    /**
     * 创建新书,初始可借册数等于总册数。
     */
    public Book(String isbn, String title, String author, int totalCopies) {
        if (totalCopies <= 0) {
            throw new IllegalArgumentException("totalCopies must be positive");
        }
        this.isbn = isbn;
        this.title = title;
        this.author = author;
        this.totalCopies = totalCopies;
        this.availableCopies = totalCopies;
    }

    /**
     * 领域层借出:可借册数减 1。
     * 并发借阅由 BookRepository 的原子条件 UPDATE 兜底,本方法用于领域不变式的封装。
     */
    public void borrow() {
        if (availableCopies <= 0) {
            throw new IllegalStateException("No copies available for book " + id);
        }
        availableCopies--;
    }

    /**
     * 领域层归还:可借册数加 1,且不超过总册数。
     */
    public void returnCopy() {
        if (availableCopies >= totalCopies) {
            throw new IllegalStateException("All copies of book " + id + " are already available");
        }
        availableCopies++;
    }
}
