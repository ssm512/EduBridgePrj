package com.edu.domain.ai.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AiPageController {

    // 관리자 대시보드의 AI 리포트 화면
    @GetMapping("/admin/ai")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminAiPage() {
        return "admin/ai/aiReport";
    }
}
