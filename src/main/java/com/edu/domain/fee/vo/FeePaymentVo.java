package com.edu.domain.fee.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * fee_payments 테이블 VO
 * 회비 납부 이력 (취소 시 cancel_yn = 'Y', 이력은 삭제하지 않는다)
 * V10: 영수증 번호는 payment_receipts.receipt_no로 이관되어 이 VO에서는 제거됨 (RCT-01/02 구현 시 참조)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeePaymentVo {

    /** PK */
    private Long paymentId;

    /** fees.fee_id FK */
    private Long feeId;

    /** 납부 금액 */
    private Long paidAmount;

    /** 납부 시각 */
    private LocalDateTime paidAt;

    /** 납부 수단 (CASH / TRANSFER / CARD) */
    private String paymentMethod;

    /** 취소 여부 (Y/N) */
    private String cancelYn;

    /** 생성 시각 */
    private LocalDateTime createdAt;

    public boolean isCanceled() {
        return "Y".equals(cancelYn);
    }
}
