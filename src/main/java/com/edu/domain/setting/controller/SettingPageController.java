package com.edu.domain.setting.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 시스템설정 화면 라우트. (/admin/** 는 SecurityConfig에서 ADMIN 전용)
 */
@Controller
public class SettingPageController {

    @GetMapping("/admin/settings")
    public String settings() {
        return "admin/settings";
    }
}
