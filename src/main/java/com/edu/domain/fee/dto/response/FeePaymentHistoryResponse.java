package com.edu.domain.fee.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 납부 이력 조회 응답 (FEE-06) - fee_payments 한 행 = 한 납부 기록
 * 취소분(cancel_yn='Y')도 포함해서 반환한다. 화면에서 '취소됨' 으로 구분 표시.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeePaymentHistoryResponse {

    /** 납부 이력 PK */
    private Long paymentId;

    /** 회비 PK */
    private Long feeId;

    /** 납부 금액 */
    private Long paidAmount;

    /** 납부 일시 */
    private LocalDateTime paidAt;

    /** 납부 수단 (CASH / TRANSFER / CARD) */
    private String paymentMethod;

    /** 취소 여부 (Y / N) */
    private String cancelYn;

    /** 취소된 납부인지 - 화면 뱃지/합계 제외 판단용 */
    public boolean isCanceled() {
        return "Y".equals(cancelYn);
    }
}
