package com.edu.domain.fee.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 납부 처리 요청 (FEE-04)
 * POST /api/v1/fees/{feeId}/payments
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeePaymentRequest {

    /** 납부 금액 */
    @NotNull(message = "납부 금액을 입력해주세요")
    @Positive(message = "납부 금액은 0보다 커야 합니다")
    private Long paidAmount;

    /** 납부 시각 (미입력 시 서버 현재 시각) */
    private LocalDateTime paidAt;

    /** 납부 수단 */
    @NotBlank(message = "납부 수단을 선택해주세요")
    @Pattern(regexp = "CASH|TRANSFER|CARD", message = "납부 수단은 CASH/TRANSFER/CARD 중 하나여야 합니다")
    private String paymentMethod;
}
