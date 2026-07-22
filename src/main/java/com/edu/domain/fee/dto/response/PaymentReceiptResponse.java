package com.edu.domain.fee.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 영수증 상세 조회 응답 (RCT-01, FEE-19/20)
 * payment_receipts + fee_payments + fees + students(users) + classes + issuer(users) JOIN 결과.
 * RCT-02(PDF) 렌더링 템플릿(fee/receiptPrint.html)에도 그대로 모델로 넘긴다 - 조회용/출력용 데이터를 이원화하지 않기 위함.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentReceiptResponse {

    /** 영수증 PK */
    private Long receiptId;

    /** 영수증 번호 (RCP + payment_id 8자리) */
    private String receiptNo;

    /** 상태 (ISSUED / CANCELLED) */
    private String statusCode;

    /** 발급 시각 */
    private LocalDateTime issuedAt;

    /** 발급자 이름 (관리자) */
    private String issuedByName;

    /** 취소 시각 (미취소면 null) */
    private LocalDateTime cancelledAt;

    /** 취소 사유 (미취소면 null, 취소되면 고정 문구) */
    private String cancelReason;

    /** 납부 이력 PK */
    private Long paymentId;

    /** 납부 금액 */
    private Long paidAmount;

    /** 납부 일시 */
    private LocalDateTime paidAt;

    /** 납부 수단 (CASH / TRANSFER / CARD) */
    private String paymentMethod;

    /** 회비 PK */
    private Long feeId;

    /** 청구 월 (YYYY-MM) */
    private String billingMonth;

    /** 학생 이름 */
    private String studentName;

    /** 학번 */
    private String studentNo;

    /** 반 이름 (개인 청구면 null) */
    private String className;

    public boolean isCancelled() {
        return "CANCELLED".equals(statusCode);
    }
}
