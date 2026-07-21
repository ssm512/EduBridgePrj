package com.edu.domain.fee.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 할인정책 계산 미리보기 응답 (FEE-10, DCP-04)
 * 저장 없이 특정 회비 금액에 정책을 적용했을 때의 할인액/청구액만 계산해서 보여준다.
 * 실제 확정(저장)은 회비 등록/수정(FeeService.createFee/updateFee) 시 discountPolicyId 를
 * 함께 넘기는 방식으로 이뤄지며, 그때 fee_discounts 에 이력이 남는다 (DCP-05).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscountPreviewResponse {

    private Long discountPolicyId;

    /** 계산에 사용한 원래 회비 금액 */
    private long feeAmount;

    /** 계산된 할인 금액 (feeAmount 를 넘지 않도록 캡됨) */
    private long discountAmount;

    /** 할인 적용 후 실제 청구 금액 (feeAmount - discountAmount) */
    private long billableAmount;
}
