package com.edu.domain.fee.service;

import com.edu.common.dto.PageResponse;
import com.edu.domain.fee.dto.request.DiscountPolicyRequest;
import com.edu.domain.fee.dto.response.DiscountPolicyResponse;
import org.springframework.security.core.Authentication;

/**
 * 할인정책 서비스 (FEE-10)
 */
public interface DiscountPolicyService {

    /** 목록 조회 - 활성만 필터 선택 + 페이징 */
    PageResponse<DiscountPolicyResponse> getList(boolean activeOnly, int page, int size);

    /** PK 단건 조회 (없으면 404) */
    DiscountPolicyResponse get(Long discountPolicyId);

    /** 등록 - created_by(등록자) 기록을 위해 Authentication 필요 */
    DiscountPolicyResponse create(DiscountPolicyRequest request, Authentication authentication);

    /** 수정 (active_yn 토글도 이 메서드로 처리 - 별도 삭제 API 없음) */
    DiscountPolicyResponse update(Long discountPolicyId, DiscountPolicyRequest request);
}
