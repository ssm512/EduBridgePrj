package com.edu.domain.fee.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 회비 등록 응답 (FEE-01)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeeCreateResponse {

    /** 생성된 회비 PK */
    private Long feeId;

    /** 등록 시 결정된 초기 상태 (SCHEDULED / UNPAID) */
    private String statusCode;
}
