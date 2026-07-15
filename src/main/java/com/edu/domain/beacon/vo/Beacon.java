package com.edu.domain.beacon.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 비콘 VO — beacons 테이블과 1:1 (BCN-01~03).
 * 스키마: V1__init_schema.sql (+ V5__add_beacon_gps.sql 의 gps 컬럼)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Beacon {

    /** PK */
    private Long beaconId;

    /** 반 (classes.class_id) */
    private Long classId;

    /** 비콘 UUID */
    private String beaconUuid;

    /** iBeacon major */
    private Integer majorValue;

    /** iBeacon minor */
    private Integer minorValue;

    /** 출석 인정 RSSI 기준 (기본 -75) */
    private Integer rssiThreshold;

    /** 설치 위치명 */
    private String locationName;

    /** 활성 여부 ('Y'/'N') */
    private String activeYn;

    /** 강의실 기준 위도 (GPS 검증용) */
    private BigDecimal gpsLatitude;

    /** 강의실 기준 경도 (GPS 검증용) */
    private BigDecimal gpsLongitude;

    /** 생성 시각 */
    private LocalDateTime createdAt;

    // ── 조회 표시용(비영속) ──
    /** 반 이름 (classes.class_name) */
    private String className;
}
