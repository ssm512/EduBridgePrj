package com.edu.domain.fee.controller;

import com.edu.common.dto.PageResponse;
import com.edu.domain.fee.dto.request.FeeSearchRequest;
import com.edu.domain.fee.dto.response.FeeListResponse;
import com.edu.domain.fee.service.FeeService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 회비 REST API (FEE-01 ~ 06)
 * base path 는 API 명세서 공통규격(/api/v1) 기준
 */
@RestController
@RequestMapping("/api/v1/fees")
public class FeeApiController {

    private final FeeService feeService;

    public FeeApiController(FeeService feeService) {
        this.feeService = feeService;
    }

    /**
     * GET /api/v1/fees - 회비 목록 조회 (FEE-02)
     * ?studentId=&billingMonth=YYYY-MM&statusCode=&page=1&size=10
     * TODO: PARENT 는 본인 자녀 회비만 조회되도록 데이터 범위 제한 필요 (팀 논의)
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'PARENT')")
    public PageResponse<FeeListResponse> getFeeList(@ModelAttribute FeeSearchRequest search) {
        return feeService.getFeeList(search);
    }
}
