package com.edu.domain.attendance.dto.response;

import com.edu.domain.attendance.vo.AttendanceRecord;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 출석 응답 DTO.
 * ATT-01/02 등록 결과, ATT-03 이력 조회 항목에 공통 사용.
 */
public record AttendanceResponse(
        Long attendanceId,
        Long studentId,
        Long classId,
        LocalDate attendanceDate,
        String statusCode,
        String checkType,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime checkedAt,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime checkOutAt,
        String failureReason
) {
    public static AttendanceResponse from(AttendanceRecord r) {
        return new AttendanceResponse(
                r.getAttendanceId(),
                r.getStudentId(),
                r.getClassId(),
                r.getAttendanceDate(),
                r.getStatusCode(),
                r.getCheckType(),
                r.getCheckedAt(),
                r.getCheckOutAt(),
                r.getFailureReason()
        );
    }
}
