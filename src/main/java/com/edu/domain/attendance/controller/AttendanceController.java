package com.edu.domain.attendance.controller;

import com.edu.common.dto.PageResponse;
import com.edu.domain.attendance.dto.request.AttendanceCheckRequest;
import com.edu.domain.attendance.dto.request.AttendanceCheckoutRequest;
import com.edu.domain.attendance.dto.request.AttendanceUpdateRequest;
import com.edu.domain.attendance.dto.request.ManualAttendanceRequest;
import com.edu.domain.attendance.dto.request.MarkAbsentRequest;
import com.edu.domain.attendance.dto.response.AttendanceResponse;
import com.edu.domain.attendance.dto.response.AttendanceStatisticsResponse;
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

    /** ATT-02 수동 출석 등록 (관리자/강사) */
    @PostMapping("/manual")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public AttendanceResponse manual(@RequestBody ManualAttendanceRequest request,
                                     Authentication authentication) {
        Long createdBy = attendanceService.getMyUserId(authentication.getName());
        return attendanceService.registerManual(request, createdBy);
    }

    /** ATT-03 출석 이력 조회 (페이징) */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','PARENT','STUDENT')")
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

    /** ATT-04 출석 수정 (관리자/강사) */
    @PutMapping("/{attendanceId}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public AttendanceResponse update(@PathVariable Long attendanceId,
                                     @RequestBody AttendanceUpdateRequest request) {
        return attendanceService.update(attendanceId, request);
    }

    /** ATT-04 출석 삭제 (관리자/강사) */
    @DeleteMapping("/{attendanceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public void delete(@PathVariable Long attendanceId) {
        attendanceService.delete(attendanceId);
    }

    /** ATT-08 결석 일괄 처리 (관리자/강사): 반+날짜의 미기록 수강생을 ABSENT로 */
    @PostMapping("/mark-absent")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
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
}
