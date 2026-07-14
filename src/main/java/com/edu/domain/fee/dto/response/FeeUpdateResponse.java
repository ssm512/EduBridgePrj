package com.edu.domain.fee.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 회비 수정 응답 (FEE-03)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeeUpdateResponse {

    /** 수정된 회비 PK */
    private Long feeId;

    /** 수정 후 재계산된 상태 */
    private String statusCode;
}
