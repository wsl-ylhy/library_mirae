package com.mirae.library.member;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 会员仓储。仅使用 JpaRepository 自带的基础方法(existsById 等)。
 */
public interface MemberRepository extends JpaRepository<Member, Long> {
}
