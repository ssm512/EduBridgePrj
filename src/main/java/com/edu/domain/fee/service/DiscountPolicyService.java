package com.edu.domain.fee.service;

import com.edu.common.dto.PageResponse;
import com.edu.domain.fee.dto.request.DiscountPolicyRequest;
import com.edu.domain.fee.dto.response.DiscountPolicyResponse;
import com.edu.domain.fee.dto.response.DiscountPreviewResponse;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;

/**
 * 할인정책 서비스 (FEE-10)
 */
public interface DiscountPolicyService {

    /**
     * 목록 조회 - activeYn(Y/N, null=전체) + targetDate(기준일, null=기간 필터 없음) + 페이징
     * targetDate 를 주면 해당 날짜가 start_date~end_date 범위 안에 있는 정책만 조회한다.
     */
    PageResponse<DiscountPolicyResponse> getList(String activeYn, LocalDate targetDate, int page, int size);

    /** PK 단건 조회 (없으면 404) */
    DiscountPolicyResponse get(Long discountPolicyId);

    /** 등록 - created_by(등록자) 기록을 위해 Authentication 필요 */
    DiscountPolicyResponse create(DiscountPolicyRequest request, Authentication authentication);

    /** 수정 (active_yn 토글도 이 메서드로 처리 - 별도 삭제 API 없음) */
    DiscountPolicyResponse update(Long discountPolicyId, DiscountPolicyRequest request);

    /**
     * 계산 미리보기 (FEE-10, DCP-04) - 저장 없이 특정 회비 금액에 정책을 적용했을 때의
     * 할인액/청구액만 계산해서 보여준다. 존재하지 않는 정책(404), 비활성/기간 밖(400) 은 예외.
     * FeeServiceImpl.createFee/updateFee 의 실제 확정 로직도 이 메서드를 재사용한다 (계산 로직 이원화 방지).
     */
    DiscountPreviewResponse preview(Long discountPolicyId, long feeAmount);
}