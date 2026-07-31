package com.edu.domain.attendance.dto.response;

/**
 * ATT-05 출석 통계 응답 (GET /attendance/statistics)
 * 출석/지각/결석/조퇴 건수 및 출석률.
 */
public record AttendanceStatisticsResponse(
        int present,
        int late,
        int absent,
        int leave,
        double attendanceRate
) {
    public static AttendanceStatisticsResponse empty() {
        return new AttendanceStatisticsResponse(0, 0, 0, 0, 0.0);
    }
}
