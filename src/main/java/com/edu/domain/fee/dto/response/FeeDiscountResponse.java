package com.edu.domain.fee.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 할인 적용 이력 조회 응답 (FEE-15/16, DCP-05) - fee_discounts 한 행 = 확정(등록/수정) 시점의 할인 스냅샷
 * getPaymentHistory 와 동일하게 회비 1건 기준 이력 전체를 최신순으로 반환한다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeeDiscountResponse {

    /** 할인 이력 PK */
    private Long feeDiscountId;

    /** 회비 PK */
    private Long feeId;

    /** 적용된 할인정책 PK (정책 삭제/비활성 이후에도 이력은 남는다) */
    private Long discountPolicyId;

    /** 적용된 할인 금액 (계산 결과 스냅샷) */
    private BigDecimal discountAmount;

    /** 적용 사유/메모 (현재 API는 별도 입력을 받지 않아 대부분 null) */
    private String reason;

    /** 적용(확정) 처리자 user_id */
    private Long appliedBy;

    /** 적용 시각 */
    private LocalDateTime createdAt;
}
