package com.edu.domain.fee.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 회비 수정 요청 (FEE-03)
 * PUT /api/v1/fees/{feeId} - 금액, 할인, 납부기한, 비고만 수정 가능
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeeUpdateRequest {

    /** 청구 금액 */
    @NotNull(message = "청구 금액을 입력해주세요")
    @PositiveOrZero(message = "청구 금액은 0 이상이어야 합니다")
    private Long feeAmount;

    /** 할인 금액 (미입력 시 0) */
    @PositiveOrZero(message = "할인 금액은 0 이상이어야 합니다")
    private Long discountAmount;

    /** 납부 기한 */
    @NotNull(message = "납부 기한을 입력해주세요")
    private LocalDate dueDate;

    /** 비고 */
    @Size(max = 255, message = "비고는 255자 이내로 입력해주세요")
    private String description;
}
