package com.edu.domain.fee.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 회비 조회 화면(페이지) 컨트롤러 - 학부모/학생용 (FEE-14).
 * FeePageController(관리자, 클래스 레벨 /admin/fees)와 베이스 경로가 달라 별도 클래스로 분리했다.
 * notice 도메인의 NoticePageController 패턴과 동일하게 뷰만 반환하고,
 * 데이터는 화면 JS 가 FeeApiController 의 GET /api/fees, GET /api/fees/{feeId}/payments 를
 * fetch 해서 채운다. 두 API 모두 JWT userId 로 "본인(자녀) 회비만" 서버가 강제 스코핑하므로
 * 이 컨트롤러에서 별도로 소유권 검증을 할 필요가 없다.
 * 범위는 "조회"까지이며, 실제 온라인 결제(PG 연동)나 본인/학부모 납부 처리는 포함하지 않는다
 * (요구사항명세서 FEE-14 원문: "학부모가 앱에서 자녀 회비 내역 조회").
 * 학생 본인 조회는 명세서 원문 범위 밖이지만, 학생 홈 화면에 이미 있던 "내 회비" 버튼을
 * 실제로 동작시키기 위해 같은 스코핑 방식(본인 user_id 기준)으로 추가했다.
 * GradePageController(성적 도메인)가 admin/teacher/student/parent 화면을 한 클래스에서
 * 클래스 레벨 매핑 없이 호스팅하는 것과 동일한 패턴을 따른다.
 */
@Controller
public class FeeParentPageController {

    /** GET /parentPage/fees - 학부모 회비 조회 화면 */
    @GetMapping("/parentPage/fees")
    @PreAuthorize("hasRole('PARENT')")
    public String parentFeesPage() {
        return "parent/fees";
    }

    /** GET /studentPage/fees - 학생 본인 회비 조회 화면 */
    @GetMapping("/studentPage/fees")
    @PreAuthorize("hasRole('STUDENT')")
    public String studentFeesPage() {
        return "student/fees";
    }
}
