package com.edu.domain.fee.service;

import com.edu.common.dto.PageResponse;
import com.edu.domain.fee.dto.request.FeeCreateRequest;
import com.edu.domain.fee.dto.request.FeePaymentRequest;
import com.edu.domain.fee.dto.request.FeeSearchRequest;
import com.edu.domain.fee.dto.request.FeeUpdateRequest;
import com.edu.domain.fee.dto.response.FeeCreateResponse;
import com.edu.domain.fee.dto.response.FeeListResponse;
import com.edu.domain.fee.dto.response.FeePaymentResponse;
import com.edu.domain.fee.dto.response.FeeUpdateResponse;

public interface FeeService {

    /** 회비 목록 조회 - 검색 조건 + 페이징 (FEE-02) */
    PageResponse<FeeListResponse> getFeeList(FeeSearchRequest search);

    /** 회비 등록 (FEE-01) */
    FeeCreateResponse createFee(FeeCreateRequest request);

    /** 회비 수정 - 금액/할인/기한/비고 (FEE-03) */
    FeeUpdateResponse updateFee(Long feeId, FeeUpdateRequest request);

    /** 납부 처리 - 이력 저장 + 상태 재계산 (FEE-04) */
    FeePaymentResponse payFee(Long feeId, FeePaymentRequest request);

    /** 납부 취소 - cancel_yn 처리 + 상태 재계산 (FEE-05) */
    FeePaymentResponse cancelPayment(Long paymentId);
}
