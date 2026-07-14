package com.edu.domain.attendance.dto.request;

/**
 * 퇴실 요청 (POST /attendance/checkout)
 * 등원과 동일하게 비콘 옆에서 호출 — GPS/비콘 신호를 함께 보내 검증한다.
 * 서버는 오늘 등원 기록을 찾아 퇴실 시각을 기록하고,
 * 수업 종료시간(classes.end_time)보다 이르면 LEAVE(조퇴)로 판정한다.
 */
public record AttendanceCheckoutRequest(
        Long classId,
        Double gpsLatitude,
        Double gpsLongitude,
        String beaconUuid,
        Integer rssiValue
) {
}
