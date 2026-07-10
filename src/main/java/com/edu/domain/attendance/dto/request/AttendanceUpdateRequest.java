package com.edu.domain.attendance.dto.request;

/**
 * ATT-04 출석 수정 요청 (PUT /attendance/{attendanceId})
 */
public record AttendanceUpdateRequest(
        String statusCode,
        String failureReason
) {
}
