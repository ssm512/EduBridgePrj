package com.edu.domain.attendance.vo;

import lombok.Data;

/**
 * 출석/퇴실 검증에 필요한 반 등록 비콘 정보(조회 전용).
 * beacons 테이블은 출석관리(BCN-01~03) 소유 — 읽어서 UUID/RSSI 기준 비교에 사용.
 */
@Data
public class BeaconView {
    private String beaconUuid;
    private Integer rssiThreshold;
}
