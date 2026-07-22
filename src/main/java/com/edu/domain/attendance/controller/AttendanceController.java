package com.edu.domain.attendance.controller;

import com.edu.common.dto.PageResponse;
import com.edu.domain.attendance.dto.request.AttendanceCheckRequest;
import com.edu.domain.attendance.dto.request.AttendanceCheckoutRequest;
import com.edu.domain.attendance.dto.request.AttendanceUpdateRequest;
import com.edu.domain.attendance.dto.request.ManualAttendanceRequest;
import com.edu.domain.attendance.dto.request.MarkAbsentRequest;
import com.edu.domain.attendance.dto.response.AttendanceResponse;
import com.edu.domain.attendance.dto.response.AttendanceStatisticsResponse;
import com.edu.domain.attendance.dto.response.ChildOptionResponse;
import com.edu.domain.attendance.dto.response.ClassOptionResponse;
import com.edu.domain.attendance.service.AttendanceService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 출석관리 REST API (틀).
 * 권한은 API 명세서(ATT-01~05) 그대로 메서드 단위 @PreAuthorize로 부여.
 * (같은 /attendance 경로라도 method별로 권한이 달라 @PreAuthorize가 적합)
 *
 * NOTE: 명세서 Base URL은 /api/v1 이지만 현재 코드 관례(/api/auth)에 맞춰 /api 로 둠.
 *       공통 프리픽스는 팀에서 정한 뒤 일괄 반영.
 */
@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;

    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    /** ATT-01 자동 출석 - 등원 (학생 앱). studentId는 JWT에서 도출(본인만). */
    @PostMapping("/check")
    @PreAuthorize("hasRole('STUDENT')")
    public AttendanceResponse check(@RequestBody AttendanceCheckRequest request,
                                    Authentication authentication) {
        return attendanceService.checkIn(request, authentication.getName());
    }

    /** 퇴실 (학생 앱): 본인 오늘 등원 기록에 퇴실시각 기록 + 조퇴 판정 */
    @PostMapping("/checkout")
    @PreAuthorize("hasRole('STUDENT')")
    public AttendanceResponse checkout(@RequestBody AttendanceCheckoutRequest request,
                                       Authentication authentication) {
        return attendanceService.checkOut(request, authentication.getName());
    }

    /** 본인 수강 반 목록 (출석 대상 선택용) */
    @GetMapping("/my-classes")
    @PreAuthorize("hasRole('STUDENT')")
    public List<ClassOptionResponse> myClasses(Authentication authentication) {
        return attendanceService.getMyClasses(authentication.getName());
    }

    /** 앱 오늘 수업: 오늘 요일 수업 있는 본인 반 + 오늘 출석상태 (학생) */
    @GetMapping("/my-today")
    @PreAuthorize("hasRole('STUDENT')")
    public List<com.edu.domain.attendance.dto.response.TodayClassResponse> myToday(Authentication authentication) {
        return attendanceService.getMyTodayClasses(authentication.getName());
    }

    /** ATT-02 수동 출석 등록 (관리자 전용 — 강사는 임의 입력 불가) */
    @PostMapping("/manual")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public AttendanceResponse manual(@RequestBody ManualAttendanceRequest request,
                                     Authentication authentication) {
        Long createdBy = attendanceService.getMyUserId(authentication.getName());
        return attendanceService.registerManual(request, createdBy);
    }

    /**
     * ATT-03 출석 이력 조회 (페이징) — 전체 범위 관리자 전용.
     * 강사는 /teacher(담당반), 학생은 /my(본인), 학부모는 /child/{id}(자녀) 스코프 엔드포인트를 사용한다.
     * (과거 전 역할 개방 + 서비스 스코프 미적용 상태라 타 학생 이력 열람이 가능했던 것을 ADMIN 전용으로 제한)
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<AttendanceResponse> history(
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return attendanceService.getHistory(studentId, classId, fromDate, toDate, keyword, page, size);
    }

    /** ATT-04 출석 수정 (관리자 전용) */
    @PutMapping("/{attendanceId}")
    @PreAuthorize("hasRole('ADMIN')")
    public AttendanceResponse update(@PathVariable Long attendanceId,
                                     @RequestBody AttendanceUpdateRequest request) {
        return attendanceService.update(attendanceId, request);
    }

    /** ATT-04 출석 삭제 (관리자 전용) */
    @DeleteMapping("/{attendanceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long attendanceId) {
        attendanceService.delete(attendanceId);
    }

    /** ATT-08 결석 일괄 처리 (관리자 전용; 강사는 /teacher/mark-absent 사용) */
    @PostMapping("/mark-absent")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Integer> markAbsent(@RequestBody MarkAbsentRequest request) {
        int count = attendanceService.markAbsent(request.classId(), request.date());
        return Map.of("markedAbsent", count);
    }

    /** ATT-05 출석 통계 (관리자/강사) */
    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public AttendanceStatisticsResponse statistics(
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return attendanceService.getStatistics(classId, studentId, fromDate, toDate);
    }

    // ===== 학생 본인 =====

    /** 내 출석 이력 (학생 본인). studentId는 JWT에서 도출 */
    @GetMapping("/my")
    @PreAuthorize("hasRole('STUDENT')")
    public PageResponse<AttendanceResponse> myHistory(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        return attendanceService.getMyHistory(authentication.getName(), fromDate, toDate, page, size);
    }

    /** 내 출석 요약 통계 (학생 본인) */
    @GetMapping("/my/statistics")
    @PreAuthorize("hasRole('STUDENT')")
    public AttendanceStatisticsResponse myStatistics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            Authentication authentication) {
        return attendanceService.getMyStatistics(authentication.getName(), fromDate, toDate);
    }

    // ===== 학부모(자녀) =====

    /** 내 자녀 목록 (학부모) */
    @GetMapping("/my-children")
    @PreAuthorize("hasRole('PARENT')")
    public List<ChildOptionResponse> myChildren(Authentication authentication) {
        return attendanceService.getMyChildren(authentication.getName());
    }

    /** 자녀 출석 이력 (학부모, 자녀 소유 검증) */
    @GetMapping("/child/{studentId}")
    @PreAuthorize("hasRole('PARENT')")
    public PageResponse<AttendanceResponse> childHistory(
            @PathVariable Long studentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        return attendanceService.getChildHistory(authentication.getName(), studentId, fromDate, toDate, page, size);
    }

    /** 자녀 출석 요약 통계 (학부모, 자녀 소유 검증) */
    @GetMapping("/child/{studentId}/statistics")
    @PreAuthorize("hasRole('PARENT')")
    public AttendanceStatisticsResponse childStatistics(
            @PathVariable Long studentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            Authentication authentication) {
        return attendanceService.getChildStatistics(authentication.getName(), studentId, fromDate, toDate);
    }

    // ===== 강사(담당반 한정) =====

    /** 내 담당반 목록 (강사) */
    @GetMapping("/teacher-classes")
    @PreAuthorize("hasRole('TEACHER')")
    public List<ClassOptionResponse> teacherClasses(Authentication authentication) {
        return attendanceService.getMyTeacherClasses(authentication.getName());
    }

    /** 담당반 출석 이력 (강사, 본인 반으로 서버에서 한정) */
    @GetMapping("/teacher")
    @PreAuthorize("hasRole('TEACHER')")
    public PageResponse<AttendanceResponse> teacherHistory(
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        return attendanceService.getTeacherHistory(authentication.getName(), classId, fromDate, toDate, keyword, page, size);
    }

    /** 담당반 출석 통계 (강사, 본인 반으로 서버에서 한정) */
    @GetMapping("/teacher/statistics")
    @PreAuthorize("hasRole('TEACHER')")
    public AttendanceStatisticsResponse teacherStatistics(
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            Authentication authentication) {
        return attendanceService.getTeacherStatistics(authentication.getName(), classId, fromDate, toDate);
    }

    /** 강사 결석 일괄 처리 (담당반만 + 수업 종료 후에만 — 서버 검증) */
    @PostMapping("/teacher/mark-absent")
    @PreAuthorize("hasRole('TEACHER')")
    public Map<String, Integer> teacherMarkAbsent(@RequestBody MarkAbsentRequest request,
                                                  Authentication authentication) {
        int count = attendanceService.markAbsentAsTeacher(authentication.getName(), request.classId(), request.date());
        return Map.of("markedAbsent", count);
    }

    /** 강사 앱 오늘 우리 반 로스터 (담당반만, 미출석 포함) */
    @GetMapping("/teacher/today-roster")
    @PreAuthorize("hasRole('TEACHER')")
    public List<com.edu.domain.attendance.dto.response.ClassRosterEntryResponse> teacherTodayRoster(
            @RequestParam Long classId, Authentication authentication) {
        return attendanceService.getTeacherClassRosterToday(authentication.getName(), classId);
    }
}
