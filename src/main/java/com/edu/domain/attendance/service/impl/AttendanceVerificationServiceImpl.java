package com.edu.domain.attendance.service.impl;

import com.edu.domain.attendance.service.AttendanceVerificationService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalTime;

/**
 * 출석 검증 로직 구현 (ATT-09 ~ ATT-13).
 * 순수 로직이라 DB 없이 단위 테스트로 검증 가능.
 */
@Service
public class AttendanceVerificationServiceImpl implements AttendanceVerificationService {

    /** 지구 반지름 (m) */
    private static final double EARTH_RADIUS_METERS = 6_371_000.0;

    /** ATT-09 GPS 위치 검증: 두 좌표 사이 거리(m)가 허용 반경 이내인지 */
    @Override
    public boolean verifyGps(BigDecimal studentLat, BigDecimal studentLng,
                             BigDecimal classLat, BigDecimal classLng, double radiusMeters) {
        if (studentLat == null || studentLng == null || classLat == null || classLng == null) {
            return false;
        }
        double distance = haversineMeters(
                studentLat.doubleValue(), studentLng.doubleValue(),
                classLat.doubleValue(), classLng.doubleValue());
        return distance <= radiusMeters;
    }

    /** ATT-10 BLE 비콘 UUID 검증: 감지 UUID가 등록 UUID와 일치(대소문자 무시) */
    @Override
    public boolean verifyBeaconUuid(String detectedUuid, String registeredUuid) {
        if (detectedUuid == null || registeredUuid == null) {
            return false;
        }
        return detectedUuid.trim().equalsIgnoreCase(registeredUuid.trim());
    }

    /** ATT-11 RSSI 신호 검증: 감지 세기가 기준값 이상(더 가까움)인지. 예: -60 >= -75 → 인정 */
    @Override
    public boolean verifyRssi(int detectedRssi, int rssiThreshold) {
        return detectedRssi >= rssiThreshold;
    }

    /**
     * ATT-12 출석 가능 시간 검증.
     * 수업 시작 기준 허용 분(allowMinutes) 이내면 PRESENT, 초과면 LATE.
     * (시작 전 도착도 PRESENT). 시간 정보가 없으면 판정 불가로 PRESENT 처리.
     */
    @Override
    public String resolveStatusByTime(LocalTime classStartTime, LocalTime checkedTime, long allowMinutes) {
        if (classStartTime == null || checkedTime == null) {
            return "PRESENT";
        }
        long minutesLate = Duration.between(classStartTime, checkedTime).toMinutes();
        if (minutesLate <= allowMinutes) {
            return "PRESENT";
        }
        return "LATE";
    }

    /** 조퇴 판정: 퇴실시각이 수업 종료시각보다 이르면 true. 정보 없으면 false */
    @Override
    public boolean isEarlyLeave(LocalTime classEndTime, LocalTime checkOutTime) {
        if (classEndTime == null || checkOutTime == null) {
            return false;
        }
        return checkOutTime.isBefore(classEndTime);
    }

    /** 두 위경도 사이 거리(m) — Haversine 공식 */
    private double haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_METERS * c;
    }
}
