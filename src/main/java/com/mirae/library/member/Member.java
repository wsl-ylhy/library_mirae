package com.mirae.library.member;

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
 * 会员(借阅人)领域实体。
 */
@Entity
@Table(name = "members")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 需要无参构造,设为 protected 防止外部直接创建
public class Member {

    /** 主键,数据库自增 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 姓名 */
    @Column(nullable = false)
    private String name;

    /** 邮箱,全局唯一 */
    @Column(nullable = false, unique = true)
    private String email;

    /**
     * 创建会员。
     */
    public Member(String name, String email) {
        this.name = name;
        this.email = email;
    }
}
