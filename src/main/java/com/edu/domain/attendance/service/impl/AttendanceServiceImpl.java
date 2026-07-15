package com.edu.domain.attendance.service.impl;

import com.edu.common.dto.PageResponse;
import com.edu.common.exception.ApiException;
import com.edu.domain.attendance.dto.request.AttendanceCheckRequest;
import com.edu.domain.attendance.dto.request.AttendanceCheckoutRequest;
import com.edu.domain.attendance.dto.request.AttendanceUpdateRequest;
import com.edu.domain.attendance.dto.request.ManualAttendanceRequest;
import com.edu.domain.attendance.dto.response.AttendanceResponse;
import com.edu.domain.attendance.dto.response.AttendanceStatisticsResponse;
import com.edu.domain.attendance.dto.response.ClassOptionResponse;
import com.edu.domain.attendance.mapper.AttendanceMapper;
import com.edu.domain.attendance.service.AttendanceService;
import com.edu.domain.attendance.service.AttendanceVerificationService;
import com.edu.domain.attendance.vo.AttendanceRecord;
import com.edu.domain.attendance.vo.BeaconView;
import com.edu.domain.attendance.vo.ClassScheduleView;
import com.edu.domain.notification.service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
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

    /** GPS 인정 반경(m) 기본값. TODO: system_settings(GPS_RADIUS_METERS)와 연동 */
    private static final double DEFAULT_GPS_RADIUS_METERS = 70;

    private final AttendanceMapper attendanceMapper;
    private final AttendanceVerificationService verificationService;
    private final NotificationService notificationService;

    public AttendanceServiceImpl(AttendanceMapper attendanceMapper,
                                 AttendanceVerificationService verificationService, NotificationService notificationService) {
        this.attendanceMapper = attendanceMapper;
        this.verificationService = verificationService;
        this.notificationService = notificationService;
    }

    /**
     * ATT-01 자동 출석.
     * 중복 확인 후 AUTO 출석기록으로 저장한다.
     *
     * TODO(협업): 완전한 검증(UGPS 반경/등록 비콘 UUID/RSSI/수업시간)은
     *      *   - 반(class)의 기준 좌표와 등록 비콘(beacons) 데이터,
     *      *   - system_settings의 GPS_RADIS_METERS / RSSI_DEFAULT_THRESHOLD / ATTENDANCE_ALLOW_MINUTES
     *   가 갖춰지면 verificationService로 연결한다.
     *   (현재 스키마엔 반의 기준 좌표 컬럼이 없어 반/설정 도메인과 합의 필요 → 지금은 PRESENT로 저장)
     */
    @Override
    public AttendanceResponse checkIn(AttendanceCheckRequest request, String loginId) {
        Long studentId = resolveStudentId(loginId);   // 본인만 출석 (요청의 studentId 무시)
        LocalDate today = LocalDate.now();
        guardDuplicate(studentId, request.classId(), today);

        // ATT-09/10/11 비콘(UUID)·RSSI·GPS 검증 (반에 등록 비콘이 있을 때만)
        guardBeacon(request.classId(), request.beaconUuid(), request.rssiValue(),
                request.gpsLatitude(), request.gpsLongitude());

        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        String checkTime = now.format(formatter);

        // 반 시작시간 대비 지각 판정 (반 정보 없으면 PRESENT로 처리 → resolveStatusByTime이 null-safe)
        ClassScheduleView schedule = attendanceMapper.findClassSchedule(request.classId());
        LocalTime startTime = schedule != null ? schedule.getStartTime() : null;
        String statusCode = verificationService.resolveStatusByTime(startTime, now.toLocalTime(), DEFAULT_ALLOW_MINUTES);

        AttendanceRecord record = AttendanceRecord.builder()
                .studentId(studentId)
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

        String studentName = attendanceMapper.getStudentName(studentId);
        String notifyStatus = statusCode.equals("PRESENT") ? "정상등원 " : "입실, 지각 ";
        String className = attendanceMapper.getClassName(request.classId());
        String notifyMsg = " : " + studentName + " 학생이 " + checkTime + "분, [" + className +  "] 강의에 " + notifyStatus + "하였습니다.";
        attendanceMapper.insert(record);
        notificationService.notifyParentsOfStudent(
                studentId, "ATTENDANCE", "등원 안내", notifyMsg);
        return AttendanceResponse.from(attendanceMapper.findById(record.getAttendanceId()));
    }

    @Override
    public AttendanceResponse checkOut(AttendanceCheckoutRequest request, String loginId) {
        Long studentId = resolveStudentId(loginId);   // 본인만 퇴실
        LocalDate today = LocalDate.now();
        AttendanceRecord record = attendanceMapper.findByStudentClassDate(
                studentId, request.classId(), today);
        if (record == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "오늘 등원 기록이 없어 퇴실할 수 없습니다");
        }
        // 퇴실 중복 방지
        if (record.getCheckOutAt() != null) {
            throw new ApiException(HttpStatus.CONFLICT, "이미 퇴실 처리되었습니다");
        }

        // 등원과 동일하게 비콘·GPS 검증
        guardBeacon(request.classId(), request.beaconUuid(), request.rssiValue(),
                request.gpsLatitude(), request.gpsLongitude());

        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        String checkTime = now.format(formatter);

        // 수업 종료시간보다 이르면 조퇴(LEAVE). 아니면 기존 상태(출석/지각) 유지
        ClassScheduleView schedule = attendanceMapper.findClassSchedule(request.classId());
        LocalTime endTime = schedule != null ? schedule.getEndTime() : null;
        String statusCode = record.getStatusCode();
        if (verificationService.isEarlyLeave(endTime, now.toLocalTime())) {
            statusCode = "LEAVE";
        }

        String studentName = attendanceMapper.getStudentName(studentId);
        String notifyStatus = statusCode.equals("LEAVE") ? "조퇴 " : "정상 퇴실 ";
        String className = attendanceMapper.getClassName(request.classId());
        String notifyMsg = " : " + studentName + " 학생이 " + checkTime + "분, [" + className +  "] 강의에서 " + notifyStatus + "하였습니다.";

        attendanceMapper.updateCheckOut(record.getAttendanceId(), now, statusCode);
        notificationService.notifyParentsOfStudent(
                studentId, "ATTENDANCE", "하원 안내", notifyMsg);
        return AttendanceResponse.from(attendanceMapper.findById(record.getAttendanceId()));
    }

    /** ATT-02 수동 출석 등록 (관리자/강사) */
    @Override
    public AttendanceResponse registerManual(ManualAttendanceRequest request, Long createdBy) {
        LocalDate date = request.attendanceDate() != null ? request.attendanceDate() : LocalDate.now();
        guardDuplicate(request.studentId(), request.classId(), date);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        LocalDateTime checkAt = request.checkInAt() != null ? request.checkInAt() : LocalDateTime.now();
        String checkTime = checkAt.format(formatter);

        AttendanceRecord record = AttendanceRecord.builder()
                .studentId(request.studentId())
                .classId(request.classId())
                .attendanceDate(date)
                .statusCode(request.statusCode())
                .checkType("MANUAL")
                .checkedAt(request.checkInAt() != null ? request.checkInAt() : LocalDateTime.now())
                .checkOutAt(request.checkOutAt())
                .failureReason(request.reason())
                .createdBy(createdBy)
                .build();

        String studentName = attendanceMapper.getStudentName(request.studentId());
        String notifyStatus = request.statusCode().equals("PRESENT") ? "정상등원 " : "입실, 지각 ";
        String className = attendanceMapper.getClassName(request.classId());
        String notifyMsg = " : " + studentName + " 학생이 " + checkTime + "분, [" + className +  "] 강의에 " + notifyStatus + "하였습니다.";

        attendanceMapper.insert(record);
        notificationService.notifyParentsOfStudent(
                request.studentId(), "ATTENDANCE", "등원 안내", notifyMsg);
        return AttendanceResponse.from(attendanceMapper.findById(record.getAttendanceId()));
    }

    /** ATT-03 출석 이력 조회 */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<AttendanceResponse> getHistory(Long studentId, Long classId,
                                                       LocalDate fromDate, LocalDate toDate, String keyword,
                                                       int page, int size) {
        // TODO: 권한별 조회 범위 제한(본인/자녀 등)은 정책 확정 후 반영
        int safePage = Math.max(page, 1);
        int safeSize = size <= 0 ? 10 : Math.min(size, 100);
        int offset = (safePage - 1) * safeSize;
        long total = attendanceMapper.countList(studentId, classId, fromDate, toDate, keyword);
        List<AttendanceResponse> items = attendanceMapper
                .findPage(studentId, classId, fromDate, toDate, keyword, safeSize, offset)
                .stream().map(AttendanceResponse::from).toList();
        return PageResponse.of(items, safePage, safeSize, total);
    }

    /** ATT-04 출석 수정 */
    @Override
    public AttendanceResponse update(Long attendanceId, AttendanceUpdateRequest request) {
        AttendanceRecord existing = attendanceMapper.findById(attendanceId);
        if (existing == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "출석기록을 찾을 수 없습니다: " + attendanceId);
        }
        attendanceMapper.updateDetail(attendanceId, request.statusCode(), request.failureReason(),
                request.checkInAt(), request.checkOutAt());
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
        List<AttendanceRecord> records = attendanceMapper.findList(studentId, classId, fromDate, toDate, null);
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
        // 출석률 = 온 사람(정상출석 + 지각 + 조퇴) / 전체. 결석만 제외.
        int attended = present + late + leave;
        double rate = total == 0 ? 0.0 : Math.round((attended * 10000.0) / total) / 100.0; // 소수 둘째자리
        return new AttendanceStatisticsResponse(present, late, absent, leave, rate);
    }

    /** 로그인 학생의 수강 반 목록 */
    @Override
    @Transactional(readOnly = true)
    public List<ClassOptionResponse> getMyClasses(String loginId) {
        return attendanceMapper.findMyClasses(loginId);
    }

    /** ATT-08 결석 일괄 처리 */
    @Override
    public int markAbsent(Long classId, LocalDate date) {
        List<Long> studentIds = attendanceMapper.findAbsentCandidates(classId, date);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String checkDate = date.format(formatter);
        for (Long studentId : studentIds) {
            AttendanceRecord record = AttendanceRecord.builder()
                    .studentId(studentId)
                    .classId(classId)
                    .attendanceDate(date)
                    .statusCode("ABSENT")
                    .checkType("MANUAL")   // 시스템 일괄(스키마상 AUTO/MANUAL만 허용)
                    .failureReason("미출석 자동 결석 처리")
                    .build();

            String studentName = attendanceMapper.getStudentName(studentId);
            String className = attendanceMapper.getClassName(classId);
            String notifyMsg = " : " + studentName + " 학생이 " + checkDate + ", [" + className +  "] 강의에 결석 하였습니다.";

            attendanceMapper.insert(record);
            notificationService.notifyParentsOfStudent(
                    studentId, "ATTENDANCE", "결석 안내", notifyMsg);
        }
        return studentIds.size();
    }

    @Override
    public Long getMyUserId(String loginId) {
        return attendanceMapper.getMyUserId(loginId);
    }

    /** 로그인 ID → 본인 student_id (없으면 403) */
    private Long resolveStudentId(String loginId) {
        Long studentId = attendanceMapper.findStudentIdByLoginId(loginId);
        if (studentId == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, "학생 계정이 아니거나 학생 정보가 없습니다");
        }
        return studentId;
    }

    /** 같은 날 같은 반 중복 출석 방지 */
    private void guardDuplicate(Long studentId, Long classId, LocalDate date) {
        if (attendanceMapper.countByStudentClassDate(studentId, classId, date) > 0) {
            throw new ApiException(HttpStatus.CONFLICT, "이미 해당 날짜에 출석 기록이 있습니다");
        }
    }

    /**
     * 비콘 검증 — 반에 등록된 활성 비콘이 있을 때만 수행.
     * 감지 UUID가 등록 UUID와 일치하고, RSSI가 기준 이상이어야 통과.
     * 등록 비콘이 없으면 검증을 생략(통과)한다.
     */
    private void guardBeacon(Long classId, String detectedUuid, Integer detectedRssi,
                             Double gpsLat, Double gpsLng) {
        BeaconView beacon = attendanceMapper.findActiveBeaconByClass(classId);
        if (beacon == null) {
            return; // 등록 비콘 없음 → 검증 생략
        }
        // ATT-10/11 비콘 UUID + RSSI
        int threshold = beacon.getRssiThreshold() != null ? beacon.getRssiThreshold() : -75;
        boolean uuidOk = verificationService.verifyBeaconUuid(detectedUuid, beacon.getBeaconUuid());
        boolean rssiOk = detectedRssi != null && verificationService.verifyRssi(detectedRssi, threshold);
        if (!uuidOk || !rssiOk) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "비콘 인증 실패 — 강의실 비콘 근처에서 다시 시도하세요");
        }
        // ATT-09 GPS 위치 (비콘에 기준 좌표가 등록된 경우에만)
        if (beacon.getGpsLatitude() != null && beacon.getGpsLongitude() != null) {
            boolean gpsOk = gpsLat != null && gpsLng != null
                    && verificationService.verifyGps(
                            BigDecimal.valueOf(gpsLat), BigDecimal.valueOf(gpsLng),
                            beacon.getGpsLatitude(), beacon.getGpsLongitude(),
                            DEFAULT_GPS_RADIUS_METERS);
            if (!gpsOk) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "위치 확인 실패 — 강의실 근처에서 다시 시도하세요");
            }
        }
    }

    private BigDecimal toBigDecimal(Double value) {
        return value == null ? null : BigDecimal.valueOf(value);
    }
}
