package com.edu.domain.attendance.dto.request;

import java.time.LocalDate;

/**
 * ATT-02 수동 출석 등록 요청 (POST /attendance/manual)
 * 관리자/강사가 출석을 수동 등록한다.
 */
public record ManualAttendanceRequest(
        Long studentId,
        Long classId,
        LocalDate attendanceDate,
        String statusCode,
        String reason
) {
}
