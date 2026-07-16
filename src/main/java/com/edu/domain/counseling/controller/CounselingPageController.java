package com.edu.domain.counseling.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CounselingPageController {

    // 관리자 대시보드의 상담관리 메뉴 화면
    @GetMapping("/admin/counseling")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public String counselingManagePage() {
        return "admin/counseling/counselingManage";
    }

    // 강사 홈에서 접근하는 상담 기록 화면
    @GetMapping("/teacherPage/counseling")
    @PreAuthorize("hasRole('TEACHER')")
    public String teacherCounselingManagePage() {
        return "teacher/counseling/counselingManage";
    }
}
