package com.edu.domain.attendance.service.impl;

import com.edu.common.exception.ApiException;
import com.edu.domain.attendance.dto.request.AttendanceCheckRequest;
import com.edu.domain.attendance.dto.request.AttendanceCheckoutRequest;
import com.edu.domain.attendance.dto.request.AttendanceUpdateRequest;
import com.edu.domain.attendance.dto.request.ManualAttendanceRequest;
import com.edu.domain.attendance.dto.response.AttendanceResponse;
import com.edu.domain.attendance.dto.response.AttendanceStatisticsResponse;
import com.edu.domain.attendance.mapper.AttendanceMapper;
import com.edu.domain.attendance.service.AttendanceService;
import com.edu.domain.attendance.service.AttendanceVerificationService;
import com.edu.domain.attendance.vo.AttendanceRecord;
import com.edu.domain.attendance.vo.ClassScheduleView;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * 출석 서비스 구현.
 * ATT-01 자동 / ATT-02 수동 / ATT-03 이력 / ATT-04 수정 / ATT-05 통계
 */
@Service
@Transactional
public class AttendanceServiceImpl implements AttendanceService {

    /** 출석 허용시간(분) 기본값. TODO: system_settings(ATTENDANCE_ALLOW_MINUTES)와 연동 */
    private static final long DEFAULT_ALLOW_MINUTES = 10;

    private final AttendanceMapper attendanceMapper;
    private final AttendanceVerificationService verificationService;

    public AttendanceServiceImpl(AttendanceMapper attendanceMapper,
                                 AttendanceVerificationService verificationService) {
        this.attendanceMapper = attendanceMapper;
        this.verificationService = verificationService;
    }

    /**
     * ATT-01 자동 출석.
     * 중복 확인 후 AUTO 출석기록으로 저장한다.
     *
     * TODO(협업): 완전한 검증(GPS 반경/등록 비콘 UUID/RSSI/수업시간)은
     *   - 반(class)의 기준 좌표와 등록 비콘(beacons) 데이터,
     *   - system_settings의 GPS_RADIUS_METERS / RSSI_DEFAULT_THRESHOLD / ATTENDANCE_ALLOW_MINUTES
     *   가 갖춰지면 verificationService로 연결한다.
     *   (현재 스키마엔 반의 기준 좌표 컬럼이 없어 반/설정 도메인과 합의 필요 → 지금은 PRESENT로 저장)
     */
    @Override
    public AttendanceResponse checkIn(AttendanceCheckRequest request) {
        LocalDate today = LocalDate.now();
        guardDuplicate(request.studentId(), request.classId(), today);

        LocalDateTime now = LocalDateTime.now();

        // 반 시작시간 대비 지각 판정 (반 정보 없으면 PRESENT로 처리 → resolveStatusByTime이 null-safe)
        ClassScheduleView schedule = attendanceMapper.findClassSchedule(request.classId());
        LocalTime startTime = schedule != null ? schedule.getStartTime() : null;
        String statusCode = verificationService.resolveStatusByTime(startTime, now.toLocalTime(), DEFAULT_ALLOW_MINUTES);

        // TODO: GPS 반경/등록 비콘 UUID/RSSI 검증도 verificationService로 연결
        //       (반 기준 좌표·등록 비콘 데이터가 갖춰지면)

        AttendanceRecord record = AttendanceRecord.builder()
                .studentId(request.studentId())
                .classId(request.classId())
                .attendanceDate(today)
                .statusCode(statusCode)
                .checkType("AUTO")
                .checkedAt(now)
                .gpsLatitude(toBigDecimal(request.gpsLatitude()))
                .gpsLongitude(toBigDecimal(request.gpsLongitude()))
                .beaconUuid(request.beaconUuid())
                .rssiValue(request.rssiValue())
                .build();

        attendanceMapper.insert(record);
        return AttendanceResponse.from(attendanceMapper.findById(record.getAttendanceId()));
    }

    @Override
    public AttendanceResponse checkOut(AttendanceCheckoutRequest request) {
        LocalDate today = LocalDate.now();
        AttendanceRecord record = attendanceMapper.findByStudentClassDate(
                request.studentId(), request.classId(), today);
        if (record == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "오늘 등원 기록이 없어 퇴실할 수 없습니다");
        }

        LocalDateTime now = LocalDateTime.now();

        // 수업 종료시간보다 이르면 조퇴(LEAVE). 아니면 기존 상태(출석/지각) 유지
        ClassScheduleView schedule = attendanceMapper.findClassSchedule(request.classId());
        LocalTime endTime = schedule != null ? schedule.getEndTime() : null;
        String statusCode = record.getStatusCode();
        if (verificationService.isEarlyLeave(endTime, now.toLocalTime())) {
            statusCode = "LEAVE";
        }

        attendanceMapper.updateCheckOut(record.getAttendanceId(), now, statusCode);
        return AttendanceResponse.from(attendanceMapper.findById(record.getAttendanceId()));
    }

    /** ATT-02 수동 출석 등록 (관리자/강사) */
    @Override
    public AttendanceResponse registerManual(ManualAttendanceRequest request, Long createdBy) {
        LocalDate date = request.attendanceDate() != null ? request.attendanceDate() : LocalDate.now();
        guardDuplicate(request.studentId(), request.classId(), date);

        AttendanceRecord record = AttendanceRecord.builder()
                .studentId(request.studentId())
                .classId(request.classId())
                .attendanceDate(date)
                .statusCode(request.statusCode())
                .checkType("MANUAL")
                .checkedAt(LocalDateTime.now())
                .failureReason(request.reason())
                .createdBy(createdBy)
                .build();

        attendanceMapper.insert(record);
        return AttendanceResponse.from(attendanceMapper.findById(record.getAttendanceId()));
    }

    /** ATT-03 출석 이력 조회 */
    @Override
    @Transactional(readOnly = true)
    public List<AttendanceResponse> getHistory(Long studentId, Long classId,
                                               LocalDate fromDate, LocalDate toDate) {
        // TODO: 권한별 조회 범위 제한(본인/자녀 등)은 정책 확정 후 반영
        return attendanceMapper.findList(studentId, classId, fromDate, toDate)
                .stream().map(AttendanceResponse::from).toList();
    }

    /** ATT-04 출석 수정 */
    @Override
    public AttendanceResponse update(Long attendanceId, AttendanceUpdateRequest request) {
        AttendanceRecord existing = attendanceMapper.findById(attendanceId);
        if (existing == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "출석기록을 찾을 수 없습니다: " + attendanceId);
        }
        attendanceMapper.updateStatus(attendanceId, request.statusCode(), request.failureReason());
        return AttendanceResponse.from(attendanceMapper.findById(attendanceId));
    }

    /** ATT-04 출석 삭제 */
    @Override
    public void delete(Long attendanceId) {
        AttendanceRecord existing = attendanceMapper.findById(attendanceId);
        if (existing == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "출석기록을 찾을 수 없습니다: " + attendanceId);
        }
        attendanceMapper.deleteById(attendanceId);
    }

    /** ATT-05 출석 통계 (상태별 집계 + 출석률) */
    @Override
    @Transactional(readOnly = true)
    public AttendanceStatisticsResponse getStatistics(Long classId, Long studentId,
                                                      LocalDate fromDate, LocalDate toDate) {
        List<AttendanceRecord> records = attendanceMapper.findList(studentId, classId, fromDate, toDate);
        int present = 0, late = 0, absent = 0, leave = 0;
        for (AttendanceRecord r : records) {
            switch (r.getStatusCode() == null ? "" : r.getStatusCode()) {
                case "PRESENT" -> present++;
                case "LATE"    -> late++;
                case "ABSENT"  -> absent++;
                case "LEAVE"   -> leave++;
                default -> { /* 무시 */ }
            }
        }
        int total = records.size();
        double rate = total == 0 ? 0.0 : Math.round((present * 10000.0) / total) / 100.0; // 소수 둘째자리
        return new AttendanceStatisticsResponse(present, late, absent, leave, rate);
    }

    /** 같은 날 같은 반 중복 출석 방지 */
    private void guardDuplicate(Long studentId, Long classId, LocalDate date) {
        if (attendanceMapper.countByStudentClassDate(studentId, classId, date) > 0) {
            throw new ApiException(HttpStatus.CONFLICT, "이미 해당 날짜에 출석 기록이 있습니다");
        }
    }

    private BigDecimal toBigDecimal(Double value) {
        return value == null ? null : BigDecimal.valueOf(value);
    }
}
