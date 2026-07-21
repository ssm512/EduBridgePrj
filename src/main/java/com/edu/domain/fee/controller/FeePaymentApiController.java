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
     * TODO(팀 확인): 명세의 cancelReason 파라미터는 저장할 컬럼이 없어 미구현 - 회의 안건
     */
    @PutMapping("/{paymentId}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public FeePaymentResponse cancelPayment(@PathVariable Long paymentId) {
        return feeService.cancelPayment(paymentId);
    }
}
