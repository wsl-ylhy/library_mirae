package com.mirae.library.config;

import com.mirae.library.book.Book;
import com.mirae.library.book.BookRepository;
import com.mirae.library.member.Member;
import com.mirae.library.member.MemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 种子数据初始化:应用启动且数据库为空时写入演示数据。
 */
@Component
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final BookRepository bookRepository;
    private final MemberRepository memberRepository;

    public DataSeeder(BookRepository bookRepository, MemberRepository memberRepository) {
        this.bookRepository = bookRepository;
        this.memberRepository = memberRepository;
    }

    /**
     * Spring Boot 启动完成后自动调用。
     */
    @Override
    public void run(ApplicationArguments args) {
        seed();
    }

    /**
     * 写入种子数据。幂等:已有数据时跳过。集成测试的 @BeforeEach 也复用它来重置数据。
     */
    public void seed() {
        if (bookRepository.count() > 0) {
            return; // 数据库非空,跳过以避免重复插入
        }
        // 2 位演示会员
        memberRepository.save(new Member("张伟", "zhang.wei@example.com"));
        memberRepository.save(new Member("李娜", "li.na@example.com"));

        // 4 本演示书籍;最后一本通过领域方法 borrow() 模拟副本全部借出,用于演示 409 场景
        bookRepository.save(new Book("978-0321125217", "Domain-Driven Design", "Eric Evans", 3));
        bookRepository.save(new Book("978-0132350884", "Clean Code", "Robert C. Martin", 2));
        bookRepository.save(new Book("978-0201616224", "The Pragmatic Programmer", "Andrew Hunt", 1));
        Book unavailable = new Book("978-1449373320", "Designing Data-Intensive Applications",
                "Martin Kleppmann", 2);
        unavailable.borrow();
        unavailable.borrow();
        bookRepository.save(unavailable);

        log.info("Seeded demo data: {} books, {} members", bookRepository.count(), memberRepository.count());
    }
}
