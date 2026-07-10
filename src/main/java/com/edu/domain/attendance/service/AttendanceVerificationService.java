package com.edu.domain.attendance.service;

import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * ★ 민욱님 시작점 ★  자동 출석 판정의 핵심 검증 로직 (ATT-09 ~ ATT-13).
 *
 * 다른 팀원 코드(학생/반/수강)에 의존하지 않는 순수 로직이라 단위 테스트로
 * 혼자 완성 가능하다. 기준값은 system_settings 시드에 이미 들어가 있음:
 *   - GPS_RADIUS_METERS       (기본 100)  : GPS 인정 반경
 *   - RSSI_DEFAULT_THRESHOLD  (기본 -75)  : BLE 신호 인정 기준
 *   - ATTENDANCE_ALLOW_MINUTES(기본 10)   : 지각/출석 허용 시간(분)
 *
 * 지금은 시그니처만 잡아둔 뼈대. 각 메서드 본문은 TODO.
 */
public interface AttendanceVerificationService {

    /** ATT-09 GPS 위치 검증: 강의실 좌표 기준 허용 반경(m) 안에 있는지 */
    boolean verifyGps(BigDecimal studentLat, BigDecimal studentLng,
                      BigDecimal classLat, BigDecimal classLng, double radiusMeters);

    /** ATT-10 BLE 비콘 UUID 검증: 감지된 UUID가 반 등록 비콘과 일치하는지 */
    boolean verifyBeaconUuid(String detectedUuid, String registeredUuid);

    /** ATT-11 RSSI 신호 검증: 감지된 세기가 기준값 이상인지 (가까운지) */
    boolean verifyRssi(int detectedRssi, int rssiThreshold);

    /** ATT-12 출석 가능 시간 검증: 수업 시작 기준 허용 분 안이면 PRESENT, 이후면 LATE 등 */
    String resolveStatusByTime(LocalTime classStartTime, LocalTime checkedTime, long allowMinutes);
}
