package com.edu.domain.beacon.dto.request;

import java.math.BigDecimal;

/**
 * 비콘 등록/수정 요청 (POST/PUT /api/beacons).
 * activeYn 미지정 시 서비스에서 'Y'로 기본 처리.
 */
public record BeaconRequest(
        Long classId,
        String beaconUuid,
        Integer majorValue,
        Integer minorValue,
        Integer rssiThreshold,
        String locationName,
        String activeYn,
        BigDecimal gpsLatitude,
        BigDecimal gpsLongitude
) {
}
