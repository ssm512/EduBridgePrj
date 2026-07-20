package com.edu.domain.dashboard.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 관리자 대시보드 화면 라우트. (/admin/** 는 SecurityConfig에서 ADMIN 전용)
 * 데이터는 각 도메인의 기존 조회 API를 프론트에서 집계(호출)한다.
 */
@Controller
public class DashboardPageController {

    @GetMapping("/admin/dashboard")
    public String dashboard() {
        return "admin/dashboard";
    }
}
