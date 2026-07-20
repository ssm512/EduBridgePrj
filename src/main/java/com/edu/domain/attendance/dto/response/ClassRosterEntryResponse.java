package com.edu.domain.attendance.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 강사 앱 "오늘 우리 반 현황" 로스터 1행.
 * 반의 ACTIVE 수강생 전원을 내려주며, 오늘 출석기록이 없으면 statusCode=null(미출석)이다.
 */
@Data
public class ClassRosterEntryResponse {
    private Long studentId;
    private String studentName;
    private String statusCode; // null = 미출석, PRESENT/LATE/LEAVE/ABSENT
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime checkedAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime checkOutAt;
}
