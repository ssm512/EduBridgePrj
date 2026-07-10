package com.edu.domain.attendance.dto.response;

import com.edu.domain.attendance.vo.AttendanceRecord;

import java.time.LocalDate;

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
                r.getFailureReason()
        );
    }
}
