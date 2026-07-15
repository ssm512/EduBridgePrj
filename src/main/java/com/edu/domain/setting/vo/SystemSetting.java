package com.edu.domain.setting.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 시스템설정 VO — system_settings 테이블과 1:1.
 * 스키마: V1__init_schema.sql (키/값 방식 운영 설정)
 */
@Data
public class SystemSetting {
    private Long settingId;
    private String settingKey;
    private String settingValue;
    private String description;
    private Long updatedBy;
    private LocalDateTime updatedAt;
}
