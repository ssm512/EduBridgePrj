package com.edu.domain.setting.dto.request;

/**
 * 설정값 수정 요청 (PUT /api/settings/{key}).
 * description은 선택(있으면 함께 갱신).
 */
public record SettingUpdateRequest(
        String settingValue,
        String description
) {
}
