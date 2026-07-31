package com.edu.domain.setting.controller;

import com.edu.domain.setting.service.SettingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 브랜딩(학원 이름) 조회 - 로그인 전에도 앱/웹이 표시할 수 있도록 공개(permitAll).
 * 멀티테넌트 시 각 학원 서버가 자기 ACADEMY_NAME 을 반환한다.
 */
@RestController
public class BrandingController {

    private final SettingService settingService;

    public BrandingController(SettingService settingService) {
        this.settingService = settingService;
    }

    @GetMapping("/api/branding")
    public Map<String, String> branding() {
        return Map.of("academyName", settingService.getAcademyName());
    }
}
