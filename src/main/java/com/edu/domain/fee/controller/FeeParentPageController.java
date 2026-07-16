package com.edu.domain.fee.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 회비 조회 화면(페이지) 컨트롤러 - 학부모용 (FEE-14).
 * FeePageController(관리자, 클래스 레벨 /admin/fees)와 베이스 경로가 달라 별도 클래스로 분리했다.
 * notice 도메인의 NoticePageController 패턴과 동일하게 뷰만 반환하고,
 * 데이터는 화면 JS 가 FeeApiController 의 GET /api/v1/fees, GET /api/v1/fees/{feeId}/payments 를
 * fetch 해서 채운다. 두 API 모두 JWT userId 로 "본인 자녀 회비만" 서버가 강제 스코핑하므로
 * 이 컨트롤러에서 별도로 학부모 소유권 검증을 할 필요가 없다.
 * 범위는 "조회"까지이며, 실제 온라인 결제(PG 연동)나 학부모 본인 납부 처리는 포함하지 않는다
 * (요구사항명세서 FEE-14 원문: "학부모가 앱에서 자녀 회비 내역 조회").
 */
@Controller
public class FeeParentPageController {

    /** GET /parentPage/fees - 학부모 회비 조회 화면 */
    @GetMapping("/parentPage/fees")
    @PreAuthorize("hasRole('PARENT')")
    public String parentFeesPage() {
        return "parent/fees";
    }
}
