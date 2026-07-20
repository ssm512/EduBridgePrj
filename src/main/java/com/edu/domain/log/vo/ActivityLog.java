package com.edu.domain.log.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 활동로그 VO — activity_logs 테이블 + 사용자명 조인.
 * 스키마: V1__init_schema.sql
 */
@Data
public class ActivityLog {
    private Long activityLogId;
    private Long userId;
    private String actionType;    // ATTENDANCE_FAIL / SETTING_CHANGED / LOGIN / CREATE ...
    private String targetTable;
    private Long targetId;
    private String ipAddress;
    private String userAgent;
    private String description;
    private LocalDateTime createdAt;

    // 조인 표시용
    private String userName;      // users.name (없으면 null)
}
