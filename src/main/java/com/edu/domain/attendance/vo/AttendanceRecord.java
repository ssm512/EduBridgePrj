package com.edu.domain.attendance.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 출석기록 VO — attendance_records 테이블과 1:1.
 * (스키마: V1__init_schema.sql)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceRecord {

    /** PK */
    private Long attendanceId;

    /** 학생 (students.student_id) */
    private Long studentId;

    /** 반 (classes.class_id) */
    private Long classId;

    /** 출석 일자 */
    private LocalDate attendanceDate;

    /** 출석 상태 (PRESENT/LATE/ABSENT/LEAVE) */
    private String statusCode;

    /** 체크 유형 (AUTO/MANUAL) */
    private String checkType;

    /** 등원(체크인) 시각 */
    private LocalDateTime checkedAt;

    /** 퇴실(체크아웃) 시각 — 조퇴 판정용 (V3에서 추가) */
    private LocalDateTime checkOutAt;

    /** GPS 위도 */
    private BigDecimal gpsLatitude;

    /** GPS 경도 */
    private BigDecimal gpsLongitude;

    /** 감지된 비콘 UUID */
    private String beaconUuid;

    /** 감지된 RSSI 값 */
    private Integer rssiValue;

    /** 실패 사유 (검증 실패 시) */
    private String failureReason;

    /** 수동 등록자 (users.user_id) */
    private Long createdBy;

    /** 생성 시각 */
    private LocalDateTime createdAt;
}
