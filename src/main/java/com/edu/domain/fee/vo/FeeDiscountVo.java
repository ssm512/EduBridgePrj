package com.edu.domain.fee.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * fee_discounts 테이블 VO (V10, FEE-15~16)
 * 회비 1건에 할인정책을 적용한 상세 내역 및 금액 이력.
 * Phase 1 범위: 회비 1건에 정책은 최대 1개만 적용하지만, 재적용/정정 시 이력을 남기기 위해 여러 행을 허용한다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeeDiscountVo {

    /** PK */
    private Long feeDiscountId;

    /** fees.fee_id FK */
    private Long feeId;

    /** discount_policies.discount_policy_id FK (정책 삭제/비활성 이후에도 이력은 남도록 nullable) */
    private Long discountPolicyId;

    /** 적용된 할인 금액 (원 단위, 정책 계산 결과 스냅샷) */
    private BigDecimal discountAmount;

    /** 적용 사유/메모 */
    private String reason;

    /** users.user_id FK - 적용(확정) 처리자 */
    private Long appliedBy;

    /** 적용 시각 */
    private LocalDateTime createdAt;
}
