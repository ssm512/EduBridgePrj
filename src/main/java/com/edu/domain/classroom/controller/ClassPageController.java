package com.edu.domain.classroom.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 반관리 화면(페이지) 컨트롤러.
 * AdminModuleController의 placeholder 매핑을 대체한다.
 * 데이터는 화면 로드 후 JS(fetch)가 반관리 REST API(/classes)를 호출해 채운다.
 */
@Controller
public class ClassPageController {

    /** GET /admin/classes - 반관리 화면 */
    @GetMapping("/admin/classes")
    @PreAuthorize("hasRole('ADMIN')")
    public String classesPage() {
        return "admin/classroom/classes";
    }
}
