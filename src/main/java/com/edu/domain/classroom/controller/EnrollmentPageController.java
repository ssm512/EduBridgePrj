package com.edu.domain.classroom.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 수강관리 화면(페이지) 컨트롤러.
 * AdminModuleController의 placeholder 매핑을 대체한다.
 * 데이터는 화면 로드 후 JS(fetch)가 수강관리 REST API(/enrollments)를 호출해 채운다.
 */
@Controller
public class EnrollmentPageController {

    /** GET /admin/enrollments - 수강관리 화면 */
    @GetMapping("/admin/enrollments")
    @PreAuthorize("hasRole('ADMIN')")
    public String enrollmentsPage() {
        return "admin/classroom/enrollments";
    }
}
