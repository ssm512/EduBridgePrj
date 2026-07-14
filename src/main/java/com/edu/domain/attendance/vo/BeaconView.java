package com.edu.domain.attendance.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 출석/퇴실 검증에 필요한 반 등록 비콘 정보(조회 전용).
 * beacons 테이블은 출석관리(BCN-01~03) 소유 — UUID/RSSI 기준 + GPS 기준좌표 비교에 사용.
 */
@Data
public class BeaconView {
    private String beaconUuid;
    private Integer rssiThreshold;
    /** 강의실 기준 좌표 (GPS 검증용, null이면 GPS 검증 생략) */
    private BigDecimal gpsLatitude;
    private BigDecimal gpsLongitude;
}
