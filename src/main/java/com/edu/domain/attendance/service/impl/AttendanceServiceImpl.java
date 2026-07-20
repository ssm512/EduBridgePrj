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
import com.edu.domain.attendance.service.AttendanceFailLogService;
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

    /** 출석 허용시간(분) 기본값 (system_settings 없을 때 폴백) */
    private static final long DEFAULT_ALLOW_MINUTES = 10;

    /** GPS 인정 반경(m) 기본값 (system_settings 없을 때 폴백) */
    private static final double DEFAULT_GPS_RADIUS_METERS = 70;

    /** RSSI 기본 기준값 (비콘에 개별 값 없을 때 폴백) */
    private static final int DEFAULT_RSSI_THRESHOLD = -75;

    private final AttendanceMapper attendanceMapper;
    private final AttendanceVerificationService verificationService;
    private final NotificationService notificationService;
    private final AttendanceFailLogService failLogService;
    private final com.edu.domain.setting.service.SettingService settingService;

    public AttendanceServiceImpl(AttendanceMapper attendanceMapper,
                                 AttendanceVerificationService verificationService,
                                 NotificationService notificationService,
                                 AttendanceFailLogService failLogService,
                                 com.edu.domain.setting.service.SettingService settingService) {
        this.attendanceMapper = attendanceMapper;
        this.verificationService = verificationService;
        this.notificationService = notificationService;
        this.failLogService = failLogService;
        this.settingService = settingService;
    }

    /** 출석 허용시간(분) — system_settings(ATTENDANCE_ALLOW_MINUTES), 없으면 폴백 */
    private long allowMinutes() {
        return settingService.getLong("ATTENDANCE_ALLOW_MINUTES", DEFAULT_ALLOW_MINUTES);
    }

    /** GPS 인정 반경(m) — system_settings(GPS_RADIUS_METERS), 없으면 폴백 */
    private double gpsRadiusMeters() {
        return settingService.getDouble("GPS_RADIUS_METERS", DEFAULT_GPS_RADIUS_METERS);
    }

    /** RSSI 기본 기준값 — system_settings(RSSI_DEFAULT_THRESHOLD), 없으면 폴백 */
    private int rssiDefaultThreshold() {
        return settingService.getInt("RSSI_DEFAULT_THRESHOLD", DEFAULT_RSSI_THRESHOLD);
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

        // 중복 출석 방지 (실패 시 활동로그 기록)
        if (attendanceMapper.countByStudentClassDate(studentId, request.classId(), today) > 0) {
            failAndLog(loginId, request.classId(), "DUPLICATE", request.beaconUuid(),
                    request.rssiValue(), request.gpsLatitude(), request.gpsLongitude(), HttpStatus.CONFLICT);
        }

        // ATT-09/10/11 비콘(UUID)·RSSI·GPS 검증 (반에 등록 비콘이 있을 때만). 실패 시 활동로그 기록 후 예외
        String failCode = beaconFailCode(request.classId(), request.beaconUuid(), request.rssiValue(),
                request.gpsLatitude(), request.gpsLongitude());
        if (failCode != null) {
            failAndLog(loginId, request.classId(), failCode, request.beaconUuid(),
                    request.rssiValue(), request.gpsLatitude(), request.gpsLongitude(), HttpStatus.BAD_REQUEST);
        }

        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        String checkTime = now.format(formatter);

        // 반 시작시간 대비 지각 판정 (반 정보 없으면 PRESENT로 처리 → resolveStatusByTime이 null-safe)
        ClassScheduleView schedule = attendanceMapper.findClassSchedule(request.classId());
        LocalTime startTime = schedule != null ? schedule.getStartTime() : null;
        String statusCode = verificationService.resolveStatusByTime(startTime, now.toLocalTime(), allowMinutes());

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
        if (record.getStatusCode().equals("ABSENT")) {
            throw new ApiException(HttpStatus.CONFLICT, "금일 결석 처리 된 강의입니다.");
        }
        // 퇴실 중복 방지
        if (record.getCheckOutAt() != null) {
            throw new ApiException(HttpStatus.CONFLICT, "이미 퇴실 처리되었습니다");
        }

        // 등원과 동일하게 비콘·GPS 검증. 실패 시 활동로그 기록 후 예외
        String failCode = beaconFailCode(request.classId(), request.beaconUuid(), request.rssiValue(),
                request.gpsLatitude(), request.gpsLongitude());
        if (failCode != null) {
            failAndLog(loginId, request.classId(), failCode, request.beaconUuid(),
                    request.rssiValue(), request.gpsLatitude(), request.gpsLongitude(), HttpStatus.BAD_REQUEST);
        }

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

    /** 앱 오늘 수업 + 오늘 출석상태 */
    @Override
    @Transactional(readOnly = true)
    public List<com.edu.domain.attendance.dto.response.TodayClassResponse> getMyTodayClasses(String loginId) {
        java.time.LocalDate today = java.time.LocalDate.now();
        String dayCode = today.getDayOfWeek()
                .getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.ENGLISH)
                .toUpperCase(java.util.Locale.ENGLISH);
        return attendanceMapper.findMyTodayClasses(loginId, dayCode, today);
    }

    // ===== 역할별 자기 범위 조회 =====

    /** 학생 본인 이력 (JWT loginId → 본인 studentId, 클라이언트 값 신뢰 안 함) */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<AttendanceResponse> getMyHistory(String loginId, LocalDate fromDate, LocalDate toDate,
                                                         int page, int size) {
        Long studentId = resolveStudentId(loginId);
        return pageHistory(studentId, null, fromDate, toDate, page, size);
    }

    /** 학생 본인 요약 통계 */
    @Override
    @Transactional(readOnly = true)
    public AttendanceStatisticsResponse getMyStatistics(String loginId, LocalDate fromDate, LocalDate toDate) {
        Long studentId = resolveStudentId(loginId);
        return getStatistics(null, studentId, fromDate, toDate);
    }

    /** 학부모 자녀 목록 */
    @Override
    @Transactional(readOnly = true)
    public List<com.edu.domain.attendance.dto.response.ChildOptionResponse> getMyChildren(String loginId) {
        return attendanceMapper.findMyChildren(loginId);
    }

    /** 학부모 자녀 이력 (자녀 소유 검증) */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<AttendanceResponse> getChildHistory(String loginId, Long studentId,
                                                            LocalDate fromDate, LocalDate toDate, int page, int size) {
        verifyChild(loginId, studentId);
        return pageHistory(studentId, null, fromDate, toDate, page, size);
    }

    /** 학부모 자녀 요약 통계 (자녀 소유 검증) */
    @Override
    @Transactional(readOnly = true)
    public AttendanceStatisticsResponse getChildStatistics(String loginId, Long studentId,
                                                           LocalDate fromDate, LocalDate toDate) {
        verifyChild(loginId, studentId);
        return getStatistics(null, studentId, fromDate, toDate);
    }

    // ===== 강사 담당반 스코프 =====

    @Override
    @Transactional(readOnly = true)
    public List<ClassOptionResponse> getMyTeacherClasses(String loginId) {
        return attendanceMapper.findMyTeacherClasses(loginId);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AttendanceResponse> getTeacherHistory(String loginId, Long classId,
                                                              LocalDate fromDate, LocalDate toDate, String keyword,
                                                              int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = size <= 0 ? 10 : Math.min(size, 100);
        int offset = (safePage - 1) * safeSize;
        long total = attendanceMapper.countTeacherList(loginId, classId, fromDate, toDate, keyword);
        List<AttendanceResponse> items = attendanceMapper
                .findTeacherPage(loginId, classId, fromDate, toDate, keyword, safeSize, offset)
                .stream().map(AttendanceResponse::from).toList();
        return PageResponse.of(items, safePage, safeSize, total);
    }

    @Override
    @Transactional(readOnly = true)
    public AttendanceStatisticsResponse getTeacherStatistics(String loginId, Long classId,
                                                             LocalDate fromDate, LocalDate toDate) {
        return toStatistics(attendanceMapper.findTeacherList(loginId, classId, fromDate, toDate, null));
    }

    @Override
    public int markAbsentAsTeacher(String loginId, Long classId, LocalDate date) {
        verifyTeacherClass(loginId, classId);
        // 강사는 "본인 수업 종료 후"에만 결석 처리 가능
        //  - 미래 날짜: 불가
        //  - 오늘: 수업 종료시각이 지나야 가능
        //  - 과거 날짜: 이미 종료된 수업이므로 허용
        LocalDate today = LocalDate.now();
        if (date.isAfter(today)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "미래 날짜는 결석 처리할 수 없습니다");
        }
        if (date.isEqual(today)) {
            ClassScheduleView schedule = attendanceMapper.findClassSchedule(classId);
            LocalTime endTime = schedule != null ? schedule.getEndTime() : null;
            if (endTime != null && LocalTime.now().isBefore(endTime)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "수업 종료 후에만 결석 처리할 수 있습니다");
            }
        }
        return markAbsent(classId, date);
    }

    /** 해당 반이 로그인 강사의 담당반이 아니면 403 */
    private void verifyTeacherClass(String loginId, Long classId) {
        if (classId == null || attendanceMapper.countTeacherClass(loginId, classId) == 0) {
            throw new ApiException(HttpStatus.FORBIDDEN, "본인 담당 반만 처리할 수 있습니다");
        }
    }

    /** 출석 레코드 목록 → 상태별 집계 통계 (getStatistics/강사 통계 공용) */
    private AttendanceStatisticsResponse toStatistics(List<AttendanceRecord> records) {
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
        int total = present + late + absent + leave;
        int attended = present + late + leave;   // 결석만 제외
        double rate = total == 0 ? 0.0 : Math.round((attended * 10000.0) / total) / 100.0;
        return new AttendanceStatisticsResponse(present, late, absent, leave, rate);
    }

    /** 해당 student가 로그인 학부모의 자녀가 아니면 403 */
    private void verifyChild(String loginId, Long studentId) {
        if (studentId == null || attendanceMapper.countChildOfParent(loginId, studentId) == 0) {
            throw new ApiException(HttpStatus.FORBIDDEN, "본인 자녀의 출석만 조회할 수 있습니다");
        }
    }

    /** 이력 페이징 공통 (getHistory와 동일 규칙) */
    private PageResponse<AttendanceResponse> pageHistory(Long studentId, Long classId,
                                                         LocalDate fromDate, LocalDate toDate, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = size <= 0 ? 10 : Math.min(size, 100);
        int offset = (safePage - 1) * safeSize;
        long total = attendanceMapper.countList(studentId, classId, fromDate, toDate, null);
        List<AttendanceResponse> items = attendanceMapper
                .findPage(studentId, classId, fromDate, toDate, null, safeSize, offset)
                .stream().map(AttendanceResponse::from).toList();
        return PageResponse.of(items, safePage, safeSize, total);
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
     * 비콘/RSSI/GPS 검증 — 반에 등록된 활성 비콘이 있을 때만 수행.
     * 통과하면 null, 실패하면 표준 실패코드를 반환한다(예외는 던지지 않음 → 호출부가 로그 저장 후 처리).
     * 등록 비콘이 없으면 검증 생략(null 반환).
     *
     * 실패코드: BEACON_UUID_MISMATCH / RSSI_TOO_LOW / GPS_OUT_OF_RANGE
     */
    private String beaconFailCode(Long classId, String detectedUuid, Integer detectedRssi,
                                  Double gpsLat, Double gpsLng) {
        BeaconView beacon = attendanceMapper.findActiveBeaconByClass(classId);
        if (beacon == null) {
            return null; // 등록 비콘 없음 → 검증 생략
        }
        // ATT-10 비콘 UUID
        if (!verificationService.verifyBeaconUuid(detectedUuid, beacon.getBeaconUuid())) {
            return "BEACON_UUID_MISMATCH";
        }
        // ATT-11 RSSI (비콘 개별 기준값 우선, 없으면 설정 기본값)
        int threshold = beacon.getRssiThreshold() != null ? beacon.getRssiThreshold() : rssiDefaultThreshold();
        if (detectedRssi == null || !verificationService.verifyRssi(detectedRssi, threshold)) {
            return "RSSI_TOO_LOW";
        }
        // ATT-09 GPS 위치 (비콘에 기준 좌표가 등록된 경우에만)
        if (beacon.getGpsLatitude() != null && beacon.getGpsLongitude() != null) {
            boolean gpsOk = gpsLat != null && gpsLng != null
                    && verificationService.verifyGps(
                            BigDecimal.valueOf(gpsLat), BigDecimal.valueOf(gpsLng),
                            beacon.getGpsLatitude(), beacon.getGpsLongitude(),
                            gpsRadiusMeters());
            if (!gpsOk) {
                return "GPS_OUT_OF_RANGE";
            }
        }
        return null;
    }

    /**
     * 출석 검증 실패 처리: 활동로그(activity_logs)에 실패 사유를 남기고 예외를 던진다.
     * 로그는 REQUIRES_NEW(별도 트랜잭션)라 이 예외로 인한 롤백에도 유실되지 않는다.
     */
    private void failAndLog(String loginId, Long classId, String failCode, String uuid,
                            Integer rssi, Double lat, Double lng, HttpStatus status) {
        Long userId = attendanceMapper.getMyUserId(loginId);
        String desc = "출석실패 [" + failCode + "] uuid=" + uuid
                + ", rssi=" + rssi + ", gps=" + lat + "," + lng;
        failLogService.record(userId, classId, desc);
        throw new ApiException(status, failMessage(failCode));
    }

    /** 실패코드 → 사용자용 안내 메시지 */
    private String failMessage(String failCode) {
        return switch (failCode) {
            case "DUPLICATE"           -> "이미 오늘 출석 기록이 있습니다.";
            case "BEACON_UUID_MISMATCH" -> "강의실 비콘이 감지되지 않았습니다. 비콘 근처에서 다시 시도하세요.";
            case "RSSI_TOO_LOW"        -> "비콘 신호가 약합니다. 비콘에 더 가까이서 다시 시도하세요.";
            case "GPS_OUT_OF_RANGE"    -> "강의실 위치를 벗어났습니다. 강의실 근처에서 다시 시도하세요.";
            default                     -> "출석 검증에 실패했습니다.";
        };
    }

    private BigDecimal toBigDecimal(Double value) {
        return value == null ? null : BigDecimal.valueOf(value);
    }
}
