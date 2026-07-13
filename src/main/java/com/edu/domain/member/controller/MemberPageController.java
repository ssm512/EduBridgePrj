package com.edu.domain.member.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 회원관리 화면(페이지) 컨트롤러.
 * AdminModuleController의 placeholder 매핑을 대체한다.
 * 데이터는 화면 로드 후 JS(fetch)가 회원관리 REST API(/users)를 호출해 채운다.
 */
@Controller
public class MemberPageController {

    /** GET /admin/members - 회원관리 화면 */
    @GetMapping("/admin/members")
    @PreAuthorize("hasRole('ADMIN')")
    public String membersPage() {
        return "admin/member/members";
    }
}
