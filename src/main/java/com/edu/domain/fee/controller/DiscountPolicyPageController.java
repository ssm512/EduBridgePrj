package com.edu.domain.fee.controller;

import com.edu.common.dto.PageGroup;
import com.edu.common.dto.PageResponse;
import com.edu.domain.fee.dto.response.DiscountPolicyResponse;
import com.edu.domain.fee.service.DiscountPolicyService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

/**
 * 할인정책 관리 화면 라우팅 (FEE-10)
 * 목록/필터/페이징은 서버 렌더링, 등록/수정 동작은 REST API(DiscountPolicyController) 호출.
 * 회비관리(FeePageController)와 동일한 패턴: 화면 컨트롤러 / API 컨트롤러 분리.
 */
@Controller
@RequestMapping("/admin/discount-policies")
public class DiscountPolicyPageController {

    private final DiscountPolicyService discountPolicyService;

    public DiscountPolicyPageController(DiscountPolicyService discountPolicyService) {
        this.discountPolicyService = discountPolicyService;
    }

    /** GET /admin/discount-policies - 할인정책 목록 화면 */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public String list(@RequestParam(required = false) String activeYn,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetDate,
                        @RequestParam(defaultValue = "1") int page,
                        Model model) {
        PageResponse<DiscountPolicyResponse> policyPage = discountPolicyService.getList(activeYn, targetDate, page, 20);

        model.addAttribute("title", "할인정책관리");
        model.addAttribute("policyPage", policyPage);
        model.addAttribute("activeYn", activeYn);
        model.addAttribute("targetDate", targetDate);
        // 페이징 바 그룹핑(10페이지 단위, 1~10/11~20 ...). 계산 로직은 PageGroup 참고
        model.addAttribute("pageGroup", PageGroup.of(policyPage.page(), policyPage.totalPages(), 10));
        return "admin/discountPolicy/discountPolicyList";
    }
}