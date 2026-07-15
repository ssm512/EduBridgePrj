package com.edu.domain.fee.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 회비 알림 배치(FEE-08 납부예정 / FEE-09 미납) 대상 회비 1건.
 * 알림 문구 구성에 필요한 최소 필드만 담는다 (납부 이력 join 없이 fees 단독 조회).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeeNotificationTargetResponse {
    private Long feeId;
    private Long studentId;
    private String billingMonth;
    private LocalDate dueDate;
    private Long billableAmount;   // fee_amount - discount_amount
}
