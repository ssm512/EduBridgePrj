package com.edu.domain.setting.controller;

import com.edu.domain.setting.dto.request.SettingUpdateRequest;
import com.edu.domain.setting.service.SettingService;
import com.edu.domain.setting.vo.SystemSetting;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 시스템설정 REST API (관리자 전용).
 * 운영값(출석 허용시간·GPS 반경·RSSI 기준 등)을 조회/수정.
 */
@RestController
@RequestMapping("/api/settings")
@PreAuthorize("hasRole('ADMIN')")
public class SettingController {

    private final SettingService settingService;

    public SettingController(SettingService settingService) {
        this.settingService = settingService;
    }

    /** 전체 설정 목록 */
    @GetMapping
    public List<SystemSetting> list() {
        return settingService.getAll();
    }

    /** 설정값 수정 */
    @PutMapping("/{key}")
    public SystemSetting update(@PathVariable String key,
                                @RequestBody SettingUpdateRequest request,
                                Authentication authentication) {
        return settingService.update(key, request, authentication.getName());
    }
}
