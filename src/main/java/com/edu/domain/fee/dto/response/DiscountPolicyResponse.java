package com.edu.domain.fee.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 할인정책 응답 (FEE-10)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscountPolicyResponse {

    private Long discountPolicyId;
    private String policyName;
    private String discountType;
    private BigDecimal discountValue;
    private LocalDate startDate;
    private LocalDate endDate;
    private String activeYn;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
