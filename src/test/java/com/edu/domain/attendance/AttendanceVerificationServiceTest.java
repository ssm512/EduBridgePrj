package com.edu.domain.attendance;

import com.edu.domain.attendance.service.impl.AttendanceVerificationServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 출석 검증 로직 단위 테스트 (DB 불필요, 혼자 실행 가능).
 * 실행: ./gradlew test  또는 IDE에서 이 클래스 Run
 */
class AttendanceVerificationServiceTest {

    private final AttendanceVerificationServiceImpl service = new AttendanceVerificationServiceImpl();

    // ── ATT-09 GPS ───────────────────────────────────────────
    @Test
    @DisplayName("GPS: 같은 좌표면 반경 안(true)")
    void gps_samePoint_inRadius() {
        BigDecimal lat = new BigDecimal("35.1796");
        BigDecimal lng = new BigDecimal("129.0756");
        assertTrue(service.verifyGps(lat, lng, lat, lng, 100));
    }

    @Test
    @DisplayName("GPS: 약 110m 떨어지면 반경 100m 밖(false), 200m면 안(true)")
    void gps_far() {
        BigDecimal classLat = new BigDecimal("35.1796");
        BigDecimal classLng = new BigDecimal("129.0756");
        // 위도 0.001도 ≈ 111m
        BigDecimal studentLat = new BigDecimal("35.1806");
        assertFalse(service.verifyGps(studentLat, classLng, classLat, classLng, 100));
        assertTrue(service.verifyGps(studentLat, classLng, classLat, classLng, 200));
    }

    @Test
    @DisplayName("GPS: 좌표가 null이면 false")
    void gps_null() {
        assertFalse(service.verifyGps(null, null, BigDecimal.ONE, BigDecimal.ONE, 100));
    }

    // ── ATT-10 BLE UUID ──────────────────────────────────────
    @Test
    @DisplayName("BLE UUID: 대소문자 무시 일치 true, 불일치/null false")
    void beaconUuid() {
        assertTrue(service.verifyBeaconUuid("ABC-123", "abc-123"));
        assertFalse(service.verifyBeaconUuid("ABC-123", "XYZ-999"));
        assertFalse(service.verifyBeaconUuid(null, "abc-123"));
    }

    // ── ATT-11 RSSI ──────────────────────────────────────────
    @Test
    @DisplayName("RSSI: 기준 이상이면 true, 미만이면 false")
    void rssi() {
        assertTrue(service.verifyRssi(-60, -75));   // 더 가까움
        assertTrue(service.verifyRssi(-75, -75));   // 경계
        assertFalse(service.verifyRssi(-80, -75));  // 더 멈
    }

    // ── ATT-12 시간 판정 ─────────────────────────────────────
    @Test
    @DisplayName("시간: 허용시간 이내 PRESENT, 초과 LATE, 시작 전 PRESENT")
    void statusByTime() {
        LocalTime start = LocalTime.of(10, 0);
        assertEquals("PRESENT", service.resolveStatusByTime(start, LocalTime.of(10, 5), 10));
        assertEquals("PRESENT", service.resolveStatusByTime(start, LocalTime.of(10, 10), 10));
        assertEquals("LATE",    service.resolveStatusByTime(start, LocalTime.of(10, 20), 10));
        assertEquals("PRESENT", service.resolveStatusByTime(start, LocalTime.of(9, 55), 10));
    }

    @Test
    @DisplayName("시간: 정보 없으면 PRESENT")
    void statusByTime_null() {
        assertEquals("PRESENT", service.resolveStatusByTime(null, null, 10));
    }

    // ── 조퇴 판정 ────────────────────────────────────────────
    @Test
    @DisplayName("조퇴: 종료 전 퇴실이면 true, 종료 시각 이후면 false, 정보없으면 false")
    void earlyLeave() {
        LocalTime end = LocalTime.of(12, 0);
        assertTrue(service.isEarlyLeave(end, LocalTime.of(11, 30)));  // 30분 일찍
        assertFalse(service.isEarlyLeave(end, LocalTime.of(12, 0)));  // 정시
        assertFalse(service.isEarlyLeave(end, LocalTime.of(12, 10))); // 이후
        assertFalse(service.isEarlyLeave(null, LocalTime.of(11, 0))); // 종료시간 모름
    }
}
