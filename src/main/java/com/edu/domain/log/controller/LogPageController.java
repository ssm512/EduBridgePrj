package com.edu.domain.log.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 활동로그 화면 라우트. (/admin/** 는 SecurityConfig에서 ADMIN 전용)
 */
@Controller
public class LogPageController {

    @GetMapping("/admin/logs")
    public String logs() {
        return "admin/logs";
    }
}
