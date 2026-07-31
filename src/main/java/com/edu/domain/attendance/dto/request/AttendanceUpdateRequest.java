package com.edu.domain.attendance.dto.request;

import java.time.LocalDateTime;

/**
 * ATT-04 출석 수정 요청 (PUT /attendance/{attendanceId})
 * 상태·사유와 함께 입실/퇴실 시각도 수정 가능. null인 항목은 변경하지 않는다.
 */
public record AttendanceUpdateRequest(
        String statusCode,
        String failureReason,
        LocalDateTime checkInAt,
        LocalDateTime checkOutAt
) {
}
