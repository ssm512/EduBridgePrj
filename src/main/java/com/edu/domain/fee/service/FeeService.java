package com.edu.domain.fee.service;

import com.edu.common.dto.PageResponse;
import com.edu.domain.fee.dto.request.FeeSearchRequest;
import com.edu.domain.fee.dto.response.FeeListResponse;

public interface FeeService {

    /** 회비 목록 조회 - 검색 조건 + 페이징 (FEE-02) */
    PageResponse<FeeListResponse> getFeeList(FeeSearchRequest search);
}
