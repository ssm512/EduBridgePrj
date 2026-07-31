package com.edu.domain.classroom.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * classes + teachers/users 조인 결과 DTO (MyBatis 매핑용)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassDto {

    /** 반 PK */
    private Long classId;

    /** 반 이름 */
    private String className;

    /** 담당 강사 PK (nullable) */
    private Long teacherId;

    /** 과목 */
    private String subject;

    /** 강의실 */
    private String classroom;

    /** 수업 시작 시간 */
    private LocalTime startTime;

    /** 수업 종료 시간 */
    private LocalTime endTime;

    /** 운영 상태 (ACTIVE/CLOSED) */
    private String statusCode;

    /** 수업 요일 CSV (MON~SUN). null이면 매일 */
    private String daysOfWeek;

    /** 등록일 */
    private LocalDateTime createdAt;

    // ----- 조인/집계 컬럼 -----

    /** 담당 강사 이름 (teachers → users 조인) */
    private String teacherName;

    /** 수강 중(ACTIVE) 학생 수 */
    private int activeStudentCount;
}
