package com.edu.domain.fee.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 월 단위 회비 집계 (FEE-06)
 * SQL GROUP BY billing_month 결과 한 행 = 한 달
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeeMonthlyStatResponse {

    /** 청구 월 (YYYY-MM) */
    private String billingMonth;

    /** 총 청구액 (할인 반영) */
    private Long totalAmount;

    /** 납부액 (유효 납부 합계 = 매출 기준, 팀 확인 완료 전 임시 기준) */
    private Long paidAmount;

    /** 납부완료 건수 */
    private Long paidCount;

    /** 미납 건수 */
    private Long unpaidCount;

    /** 예정 건수 */
    private Long scheduledCount;

    /** 미납액 = 총 청구액 - 납부액 */
    public Long getUnpaidAmount() {
        long total = totalAmount == null ? 0L : totalAmount;
        long paid = paidAmount == null ? 0L : paidAmount;
        return total - paid;
    }

    /** 데이터가 없는 달을 0 으로 채울 때 사용 */
    public static FeeMonthlyStatResponse empty(String billingMonth) {
        return FeeMonthlyStatResponse.builder()
                .billingMonth(billingMonth)
                .totalAmount(0L)
                .paidAmount(0L)
                .paidCount(0L)
                .unpaidCount(0L)
                .scheduledCount(0L)
                .build();
    }
}
