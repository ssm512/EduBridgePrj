package com.edu.domain.fee.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 납부 처리/취소 응답 (FEE-04, 05)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeePaymentResponse {

    /** 납부 이력 PK */
    private Long paymentId;

    /** 처리 후 회비 상태 (PAID / UNPAID / SCHEDULED) */
    private String statusCode;
}
