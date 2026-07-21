package com.edu.domain.fee.controller;

import com.edu.common.dto.PageResponse;
import com.edu.domain.fee.dto.request.DiscountPolicyRequest;
import com.edu.domain.fee.dto.response.DiscountPolicyResponse;
import com.edu.domain.fee.service.DiscountPolicyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 할인정책 REST API (FEE-10)
 * base path 는 API 명세서 공통규격(/api/v1) 기준, 회비(fees)와 같은 도메인 패키지.
 * 회비 등록/수정이 ADMIN 전용인 것과 동일하게, 정책 조회/관리도 ADMIN 전용으로 제한한다.
 * 삭제 API 는 제공하지 않는다 - 이미 회비에 참조된 정책이 있을 수 있어 active_yn 토글(PUT)로만 비활성화한다.
 */
@RestController
@RequestMapping("/api/v1/discount-policies")
@PreAuthorize("hasRole('ADMIN')")
public class DiscountPolicyController {

    private final DiscountPolicyService discountPolicyService;

    public DiscountPolicyController(DiscountPolicyService discountPolicyService) {
        this.discountPolicyService = discountPolicyService;
    }

    /** GET /api/v1/discount-policies?activeOnly=&page=&size= - 목록 조회 */
    @GetMapping
    public PageResponse<DiscountPolicyResponse> getList(@RequestParam(defaultValue = "false") boolean activeOnly,
                                                         @RequestParam(defaultValue = "1") int page,
                                                         @RequestParam(defaultValue = "10") int size) {
        return discountPolicyService.getList(activeOnly, page, size);
    }

    /** GET /api/v1/discount-policies/{policyId} - 단건 조회 */
    @GetMapping("/{policyId}")
    public DiscountPolicyResponse get(@PathVariable Long policyId) {
        return discountPolicyService.get(policyId);
    }

    /** POST /api/v1/discount-policies - 등록 */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DiscountPolicyResponse create(@Valid @RequestBody DiscountPolicyRequest request) {
        return discountPolicyService.create(request);
    }

    /** PUT /api/v1/discount-policies/{policyId} - 수정 (활성/비활성 전환 포함) */
    @PutMapping("/{policyId}")
    public DiscountPolicyResponse update(@PathVariable Long policyId,
                                         @Valid @RequestBody DiscountPolicyRequest request) {
        return discountPolicyService.update(policyId, request);
    }
}
