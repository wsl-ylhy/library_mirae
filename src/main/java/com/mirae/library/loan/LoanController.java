package com.mirae.library.loan;

import com.mirae.library.common.error.InvalidRequestException;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 借阅 REST 接口。
 */
@RestController
@RequestMapping("/api")
public class LoanController {

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    /**
     * POST /api/loans —— 借书。成功返回 201 + Location 头(指向新创建的借阅资源)。
     */
    @PostMapping("/loans")
    public ResponseEntity<LoanResponse> borrow(@Valid @RequestBody CreateLoanRequest request) {
        LoanResponse loan = loanService.borrow(request.memberId(), request.bookId());
        return ResponseEntity.created(URI.create("/api/loans/" + loan.id())).body(loan);
    }

    /**
     * POST /api/loans/{loanId}/return —— 归还书籍。
     */
    @PostMapping("/loans/{loanId}/return")
    public LoanResponse returnBook(@PathVariable Long loanId) {
        return loanService.returnBook(loanId);
    }

    /**
     * GET /api/members/{memberId}/loans —— 借阅列表,默认返回当前借阅(active)。
     */
    @GetMapping("/members/{memberId}/loans")
    public List<LoanResponse> loansOfMember(@PathVariable Long memberId,
                                            @RequestParam(required = false) String status) {
        return loanService.loansOfMember(memberId, parseStatus(status));
    }

    /**
     * 解析 status 查询参数:null/active → ACTIVE,returned → RETURNED,其他取值 → 400。
     */
    private static LoanStatus parseStatus(String status) {
        if (status == null || status.equalsIgnoreCase(LoanStatus.ACTIVE.name())) {
            return LoanStatus.ACTIVE; // 默认只看当前借阅
        }
        if (status.equalsIgnoreCase(LoanStatus.RETURNED.name())) {
            return LoanStatus.RETURNED;
        }
        throw new InvalidRequestException("status must be 'active' or 'returned'"); // 非法取值 → 400
    }
}
