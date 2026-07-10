package com.edu.domain.attendance.service.impl;

import com.edu.domain.attendance.service.AttendanceVerificationService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * 출석 검증 로직 구현 (뼈대).
 * TODO: 각 메서드 실제 판정 로직 + 단위 테스트 작성.
 */
@Service
public class AttendanceVerificationServiceImpl implements AttendanceVerificationService {

    @Override
    public boolean verifyGps(BigDecimal studentLat, BigDecimal studentLng,
                             BigDecimal classLat, BigDecimal classLng, double radiusMeters) {
        // TODO: Haversine 공식으로 두 좌표 거리(m) 계산 후 radiusMeters 이내인지 반환
        return false;
    }

    @Override
    public boolean verifyBeaconUuid(String detectedUuid, String registeredUuid) {
        // TODO: null 안전 비교 (대소문자 무시 등 정책 결정)
        return false;
    }

    @Override
    public boolean verifyRssi(int detectedRssi, int rssiThreshold) {
        // TODO: detectedRssi >= rssiThreshold (예: -60 >= -75 → 인정)
        return false;
    }

    @Override
    public String resolveStatusByTime(LocalTime classStartTime, LocalTime checkedTime, long allowMinutes) {
        // TODO: 허용시간 내 → PRESENT, 초과 → LATE, (수업 종료 후 등 조건은 정책에 따라)
        return null;
    }
}
