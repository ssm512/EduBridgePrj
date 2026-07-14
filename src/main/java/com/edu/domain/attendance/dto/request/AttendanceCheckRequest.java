package com.edu.domain.attendance.dto.request;

/**
 * ATT-01 자동 출석 요청 (POST /attendance/check)
 * 학생 앱에서 GPS/BLE 정보를 전송한다.
 * studentId는 보내지 않는다 — 서버가 로그인한 사용자(JWT)에서 도출한다(본인만 출석).
 */
public record AttendanceCheckRequest(
        Long classId,
        Double gpsLatitude,
        Double gpsLongitude,
        String beaconUuid,
        Integer rssiValue
) {
}
