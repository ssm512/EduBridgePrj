package com.edu.common.web;

import com.edu.domain.setting.service.SettingService;
import org.springframework.stereotype.Component;

/**
 * Thymeleaf 템플릿에서 학원 이름을 쓰기 위한 헬퍼 빈.
 * 사용: &lt;title th:text="${@branding.name} + ' · 관리자'"&gt;...&lt;/title&gt;
 * (페이지 렌더 시에만 호출되므로 REST 요청엔 부하가 없다.)
 */
@Component("branding")
public class BrandingBean {

    private final SettingService settingService;

    public BrandingBean(SettingService settingService) {
        this.settingService = settingService;
    }

    /** 학원 이름 (미설정 시 'EduBridge') */
    public String getName() {
        return settingService.getAcademyName();
    }
}
