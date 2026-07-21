package com.edu.domain.fee.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 할인정책 등록/수정 요청 (FEE-10)
 * POST /api/v1/discount-policies, PUT /api/v1/discount-policies/{policyId}
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscountPolicyRequest {

    /** 정책명 (예: 형제·자매 할인) */
    @NotBlank(message = "정책명을 입력해주세요")
    @Size(max = 100, message = "정책명은 100자 이내로 입력해주세요")
    private String policyName;

    /** RATE(정률) / FIXED(정액) */
    @NotBlank(message = "할인 유형을 선택해주세요")
    @Pattern(regexp = "^(RATE|FIXED)$", message = "할인 유형은 RATE 또는 FIXED 여야 합니다")
    private String discountType;

    /** RATE 이면 0~100(%), FIXED 이면 원 단위 금액 */
    @NotNull(message = "할인 값을 입력해주세요")
    @PositiveOrZero(message = "할인 값은 0 이상이어야 합니다")
    private BigDecimal discountValue;

    /** SIBLING / LONG_TERM / MULTI_CLASS / MANUAL 등 - 분류 표기용 (자동 적용판정 없음) */
    @NotBlank(message = "적용 조건 구분을 선택해주세요")
    @Size(max = 20, message = "적용 조건 구분은 20자 이내로 입력해주세요")
    private String conditionType;

    /** 적용 시작일 (미입력 시 제한 없음) */
    private LocalDate startDate;

    /** 적용 종료일 (미입력 시 제한 없음) */
    private LocalDate endDate;

    /** Y/N (미입력 시 Y) */
    @Pattern(regexp = "^[YN]$", message = "활성 여부는 Y 또는 N 이어야 합니다")
    private String activeYn;

    /** 설명 */
    @Size(max = 255, message = "설명은 255자 이내로 입력해주세요")
    private String description;
}
