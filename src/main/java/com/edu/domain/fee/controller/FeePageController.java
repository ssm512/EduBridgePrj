package com.edu.domain.fee.controller;

import com.edu.common.dto.PageGroup;
import com.edu.common.dto.PageResponse;
import com.edu.domain.fee.dto.request.FeeSearchRequest;
import com.edu.domain.fee.dto.response.FeeListResponse;
import com.edu.domain.fee.service.FeeService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 회비 관리 화면 라우팅 (SCR-W-11)
 * 목록/필터/페이징은 서버 렌더링, 납부 처리 등 동작은 REST API(FeeApiController) 호출
 */
@Controller
@RequestMapping("/admin/fees")
public class FeePageController {

    private final FeeService feeService;

    public FeePageController(FeeService feeService) {
        this.feeService = feeService;
    }

    /** GET /admin/fees - 회비 목록 화면 */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public String feeList(@ModelAttribute("search") FeeSearchRequest search, Model model) {
        PageResponse<FeeListResponse> feePage = feeService.getFeeList(search);

        model.addAttribute("title", "회비관리");
        model.addAttribute("feePage", feePage);
        // 페이징 바 그룹핑(10페이지 단위, 1~10/11~20 ...). 계산 로직은 PageGroup 참고
        model.addAttribute("pageGroup", PageGroup.of(feePage.page(), feePage.totalPages(), 10));
        return "admin/fee/feeList";
    }

    /**
     * GET /admin/fees/statistics - 회비 통계/매출 화면 (SCR-W-12)
     * 데이터는 화면 JS 가 FEE-06 API 를 fetch 해서 차트를 그린다 (서버 렌더링 아님)
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public String feeStatistics(Model model) {
        model.addAttribute("title", "회비 통계/매출");
        return "admin/fee/feeStats";
    }
}
