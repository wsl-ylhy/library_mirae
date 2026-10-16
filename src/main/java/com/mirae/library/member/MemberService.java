package com.mirae.library.member;

import com.mirae.library.common.error.MemberNotFoundException;
import org.springframework.stereotype.Service;

/**
 * 会员查询服务。目前仅提供存在性校验,供借阅流程使用。
 */
@Service
public class MemberService {

    private final MemberRepository memberRepository;

    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    /**
     * 校验会员存在,不存在时抛 MemberNotFoundException(映射为 404)。
     */
    public void requireExists(Long memberId) {
        if (!memberRepository.existsById(memberId)) {
            throw new MemberNotFoundException(memberId);
        }
    }
}
