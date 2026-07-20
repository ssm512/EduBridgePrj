package com.edu.domain.attendance.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 학생 앱 "오늘 수업" 항목.
 * 오늘 요일에 수업이 있는 본인 수강 반 + 오늘 출석 상태.
 * statusCode == null 이면 미출석(입실 전).
 * MyBatis가 직접 매핑하므로 @Data 클래스로 둔다.
 */
@Data
public class TodayClassResponse {
    private Long classId;
    private String className;
    private String statusCode;   // PRESENT/LATE/LEAVE/ABSENT, null이면 미출석
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime checkedAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime checkOutAt;
}
