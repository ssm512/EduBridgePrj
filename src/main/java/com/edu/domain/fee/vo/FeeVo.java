package com.edu.domain.fee.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * fees 테이블 VO
 * 학생별 월 회비 청구 정보 (납부 이력은 fee_payments 별도 관리)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeeVo {

    /** PK */
    private Long feeId;

    /** students.student_id FK */
    private Long studentId;

    /** classes.class_id FK (반 단위 청구 시) */
    private Long classId;

    /** 청구 월 (YYYY-MM) */
    private String billingMonth;

    /** 청구 금액 */
    private Long feeAmount;

    /** 할인 금액 */
    private Long discountAmount;

    /** discount_policies.discount_policy_id FK (선택 - 정책 미적용 시 null) */
    private Long discountPolicyId;

    /** 납부 기한 */
    private LocalDate dueDate;

    /** 상태 (PAID / UNPAID / SCHEDULED) */
    private String statusCode;

    /** 비고 */
    private String description;

    /** 생성 시각 */
    private LocalDateTime createdAt;

    /** 실 청구액 = 청구 금액 - 할인 금액 */
    public long getBillableAmount() {
        long fee      = feeAmount      == null ? 0L : feeAmount;
        long discount = discountAmount == null ? 0L : discountAmount;
        return fee - discount;
    }
}