package com.edu.domain.fee.controller;

import com.edu.common.dto.PageResponse;
import com.edu.domain.fee.dto.request.DiscountPolicyRequest;
import com.edu.domain.fee.dto.response.DiscountPolicyResponse;
import com.edu.domain.fee.dto.response.DiscountPreviewResponse;
import com.edu.domain.fee.service.DiscountPolicyService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 할인정책 REST API (FEE-10)
 * base path 는 API 명세서 공통규격(/api) 기준, 회비(fees)와 같은 도메인 패키지.
 * 회비 등록/수정이 ADMIN 전용인 것과 동일하게, 정책 조회/관리도 ADMIN 전용으로 제한한다.
 * 삭제 API 는 제공하지 않는다 - 이미 회비에 참조된 정책이 있을 수 있어 active_yn 토글(PUT)로만 비활성화한다.
 */
@RestController
@RequestMapping("/api/discount-policies")
@PreAuthorize("hasRole('ADMIN')")
public class DiscountPolicyController {

    private final DiscountPolicyService discountPolicyService;

    public DiscountPolicyController(DiscountPolicyService discountPolicyService) {
        this.discountPolicyService = discountPolicyService;
    }

    /** GET /api/discount-policies?activeYn=&targetDate=&page=&size= - 목록 조회 */
    @GetMapping
    public PageResponse<DiscountPolicyResponse> getList(
            @RequestParam(required = false) String activeYn,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return discountPolicyService.getList(activeYn, targetDate, page, size);
    }

    /** GET /api/discount-policies/{discountPolicyId} - 단건 조회 */
    @GetMapping("/{discountPolicyId}")
    public DiscountPolicyResponse get(@PathVariable Long discountPolicyId) {
        return discountPolicyService.get(discountPolicyId);
    }

    /** POST /api/discount-policies - 등록 (등록자 = 로그인 사용자) */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DiscountPolicyResponse create(@Valid @RequestBody DiscountPolicyRequest request,
                                          Authentication authentication) {
        return discountPolicyService.create(request, authentication);
    }

    /** PUT /api/discount-policies/{discountPolicyId} - 수정 (활성/비활성 전환 포함) */
    @PutMapping("/{discountPolicyId}")
    public DiscountPolicyResponse update(@PathVariable Long discountPolicyId,
                                          @Valid @RequestBody DiscountPolicyRequest request) {
        return discountPolicyService.update(discountPolicyId, request);
    }

    /**
     * GET /api/discount-policies/{discountPolicyId}/preview?feeAmount= - 계산 미리보기 (DCP-04)
     * 회비 등록/수정 화면에서 정책을 선택했을 때 실제 저장 전에 할인액/청구액을 보여주기 위한 조회 전용 API.
     * 저장은 하지 않으며, 실제 확정은 회비 등록/수정(POST·PUT /api/fees) 시 discountPolicyId 를
     * 함께 넘기는 방식으로 이뤄진다 (그때 fee_discounts 에 이력이 남음, DCP-05).
     */
    @GetMapping("/{discountPolicyId}/preview")
    public DiscountPreviewResponse preview(@PathVariable Long discountPolicyId,
                                            @RequestParam long feeAmount) {
        return discountPolicyService.preview(discountPolicyId, feeAmount);
    }
}