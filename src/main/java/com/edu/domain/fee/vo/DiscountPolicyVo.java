package com.edu.domain.fee.vo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 할인정책 (FEE-10). 관리자가 등록/관리하며, 회비 생성·수정 시 학생/반에 선택 적용한다.
 * 실제 적용 대상 선정은 관리자가 UI에서 직접 정책을 선택하는 반자동 방식이다 (Phase 1 범위).
 * V10 테이블정의서 반영: PK를 discountPolicyId로 리네이밍, conditionType 제거, createdBy/updatedAt 추가.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscountPolicyVo {

    private Long discountPolicyId;
    private String policyName;
    private String discountType;      // RATE / FIXED
    private BigDecimal discountValue; // RATE: 0~100(%), FIXED: 원 단위
    private LocalDate startDate;      // null = 시작 제한 없음
    private LocalDate endDate;        // null = 종료 제한 없음
    private String activeYn;          // Y / N
    private String description;
    private Long createdBy;           // users.user_id FK - 등록자
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public boolean isActive() {
        return "Y".equals(activeYn);
    }

    /** 기준일이 정책 적용 기간 내인지 (start/end 가 null 이면 해당 방향 제한 없음) */
    public boolean isWithinPeriod(LocalDate referenceDate) {
        if (referenceDate == null) {
            return true;
        }
        if (startDate != null && referenceDate.isBefore(startDate)) {
            return false;
        }
        return endDate == null || !referenceDate.isAfter(endDate);
    }

    /**
     * 기본 회비 금액에 이 정책을 적용했을 때의 할인 금액을 계산한다.
     * feeAmount 상한 캡(할인액이 회비를 초과하지 않도록) 은 호출측(FeeService)에서 처리한다.
     */
    public long calculateDiscountAmount(long feeAmount) {
        if (discountValue == null || feeAmount <= 0) {
            return 0L;
        }
        if ("RATE".equals(discountType)) {
            return BigDecimal.valueOf(feeAmount)
                    .multiply(discountValue)
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN)
                    .longValue();
        }
        // FIXED
        return discountValue.setScale(0, RoundingMode.DOWN).longValue();
    }
}
