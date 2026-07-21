package com.edu.domain.fee.controller;

import com.edu.domain.fee.dto.response.FeePaymentResponse;
import com.edu.domain.fee.service.FeeService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 납부 이력 REST API
 * 리소스가 fee 가 아니라 fee-payment 이므로 base path 를 분리 (API 명세서 FEE-05 경로 기준)
 */
@RestController
@RequestMapping("/api/fee-payments")
public class FeePaymentApiController {

    private final FeeService feeService;

    public FeePaymentApiController(FeeService feeService) {
        this.feeService = feeService;
    }

    /**
     * PUT /api/fee-payments/{paymentId}/cancel - 납부 취소 (FEE-05)
     * 이력 삭제가 아닌 cancel_yn = 'Y' 처리 후 회비 상태 재계산
     * 결정사항(2026-07-21): 명세의 cancelReason 파라미터는 의도적으로 미구현.
     * DB에 저장할 컬럼도 없고, MVP 스코프에서 취소 사유 기록은 불필요하다고 판단해 뺌.
     * (감사 추적이 필요해지면 fee_discounts 처럼 별도 이력 테이블/컬럼 추가로 재검토)
     */
    @PutMapping("/{paymentId}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public FeePaymentResponse cancelPayment(@PathVariable Long paymentId) {
        return feeService.cancelPayment(paymentId);
    }
}
