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

import java.time.LocalDate;

/**
 * 회비 등록 요청 (FEE-01)
 * POST /api/v1/fees
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeeCreateRequest {

    /** 학생 PK */
    @NotNull(message = "학생을 선택해주세요")
    private Long studentId;

    /** 반 PK (개인 청구 시 null 허용) */
    private Long classId;

    /** 청구 월 (YYYY-MM) */
    @NotBlank(message = "청구 월을 입력해주세요")
    @Pattern(regexp = "^\\d{4}-\\d{2}$", message = "청구 월은 YYYY-MM 형식이어야 합니다")
    private String billingMonth;

    /** 청구 금액 */
    @NotNull(message = "청구 금액을 입력해주세요")
    @PositiveOrZero(message = "청구 금액은 0 이상이어야 합니다")
    private Long feeAmount;

    /** 할인 금액 (미입력 시 0). discountPolicyId 와 함께 오면 discountPolicyId 로 자동계산한 금액이 우선한다 */
    @PositiveOrZero(message = "할인 금액은 0 이상이어야 합니다")
    private Long discountAmount;

    /** 적용할 할인정책 PK (선택). 지정 시 discountAmount 를 무시하고 정책 기준으로 자동계산한다 */
    private Long discountPolicyId;

    /** 납부 기한 */
    @NotNull(message = "납부 기한을 입력해주세요")
    private LocalDate dueDate;

    /** 비고 */
    @Size(max = 255, message = "비고는 255자 이내로 입력해주세요")
    private String description;
}
