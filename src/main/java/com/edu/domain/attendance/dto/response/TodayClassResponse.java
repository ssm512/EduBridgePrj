package com.edu.domain.attendance.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 학생 앱 "오늘 수업" 항목.
 * 오늘 요일에 수업이 있는 본인 수강 반 + 반 정보(강사·시간·강의실) + 오늘 출석 상태.
 * statusCode == null 이면 미출석(입실 전).
 * MyBatis가 직접 매핑하므로 @Data 클래스로 둔다.
 */
@Data
public class TodayClassResponse {
    private Long classId;
    private String className;
    private String teacherName;   // 담당 강사 (없으면 null)
    private String classroom;     // 강의실 (없으면 null)
    @JsonFormat(pattern = "HH:mm")
    private LocalTime startTime;
    @JsonFormat(pattern = "HH:mm")
    private LocalTime endTime;
    private String statusCode;    // PRESENT/LATE/LEAVE/ABSENT, null이면 미출석
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime checkedAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime checkOutAt;
}
