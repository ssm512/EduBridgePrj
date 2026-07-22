package com.edu.domain.fee.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * payment_receipts 테이블 VO (V10)
 * fee_payments 1건당 영수증 0~1건 (uk_receipt_payment UNIQUE(payment_id)).
 * FEE-19: 납부 처리(payFee) 시 서비스가 자동으로 1행을 생성한다. 명세에 별도 발급(POST) API가 없음.
 * FEE-20: 납부 취소(cancelPayment) 시 서비스가 자동으로 CANCELLED 로 전환한다(취소 사유는 고정 문구,
 *         결정사항 2026-07-22: fee_payments 자체는 취소 사유를 입력받지 않기로 한 기존 결정을 유지).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentReceiptVo {

    /** PK */
    private Long receiptId;

    /** fee_payments.payment_id FK, UNIQUE (1:1) */
    private Long paymentId;

    /** 영수증 번호 - "RCP" + payment_id 8자리 zero-pad. payment_id 가 이미 UNIQUE 라 구조적으로 중복이 나지 않는다 */
    private String receiptNo;

    /** 상태 (ISSUED / CANCELLED) */
    private String statusCode;

    /** 발급 시각 */
    private LocalDateTime issuedAt;

    /** 발급자(관리자) users.user_id FK */
    private Long issuedBy;

    /** 취소 시각 (취소 전에는 null) */
    private LocalDateTime cancelledAt;

    /** 취소 사유 (취소 전에는 null, 취소되면 고정 문구가 채워짐) */
    private String cancelReason;

    public boolean isCancelled() {
        return "CANCELLED".equals(statusCode);
    }
}
