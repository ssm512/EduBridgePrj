package com.edu.domain.attendance.dto.request;

/**
 * ATT-01 자동 출석 요청 (POST /attendance/check)
 * 학생 앱에서 GPS/BLE 정보를 전송한다.
 */
public record AttendanceCheckRequest(
        Long studentId,
        Long classId,
        Double gpsLatitude,
        Double gpsLongitude,
        String beaconUuid,
        Integer rssiValue
) {
}
