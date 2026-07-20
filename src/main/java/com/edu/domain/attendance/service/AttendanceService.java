package com.edu.domain.attendance.service;

import com.edu.domain.attendance.dto.request.AttendanceCheckRequest;
import com.edu.domain.attendance.dto.request.AttendanceCheckoutRequest;
import com.edu.domain.attendance.dto.request.AttendanceUpdateRequest;
import com.edu.domain.attendance.dto.request.ManualAttendanceRequest;
import com.edu.common.dto.PageResponse;
import com.edu.domain.attendance.dto.response.AttendanceResponse;
import com.edu.domain.attendance.dto.response.AttendanceStatisticsResponse;
import com.edu.domain.attendance.dto.response.ClassOptionResponse;

import java.time.LocalDate;
import java.util.List;

/**
 * 출석 서비스 (틀). 각 메서드 본문은 담당자가 구현.
 * ATT-01 자동 / ATT-02 수동 / ATT-03 이력 / ATT-04 수정 / ATT-05 통계
 */
public interface AttendanceService {

    /** ATT-01 자동 출석 (등원): loginId로 본인 학생 도출 → PRESENT/LATE 판정 후 저장 */
    AttendanceResponse checkIn(AttendanceCheckRequest request, String loginId);

    /** 퇴실: 본인 오늘 등원 기록에 퇴실시각 기록 + 종료시간보다 이르면 LEAVE(조퇴) */
    AttendanceResponse checkOut(AttendanceCheckoutRequest request, String loginId);

    /** 로그인 학생이 수강 중인 반 목록 (출석 대상 선택용) */
    List<ClassOptionResponse> getMyClasses(String loginId);

    /** 앱 오늘 수업: 오늘 요일 수업 있는 본인 반 + 오늘 출석상태 */
    List<com.edu.domain.attendance.dto.response.TodayClassResponse> getMyTodayClasses(String loginId);

    /** ATT-08 결석 일괄 처리: 해당 반의 그 날짜 미기록 수강생을 ABSENT로 등록. 등록 건수 반환 */
    int markAbsent(Long classId, LocalDate date);

    /** ATT-02 수동 출석 등록 */
    AttendanceResponse registerManual(ManualAttendanceRequest request, Long createdBy);

    /** ATT-03 출석 이력 조회 (keyword = 학생 이름 부분 일치, 페이징) */
    PageResponse<AttendanceResponse> getHistory(Long studentId, Long classId,
                                                LocalDate fromDate, LocalDate toDate, String keyword,
                                                int page, int size);

    /** ATT-04 출석 수정 */
    AttendanceResponse update(Long attendanceId, AttendanceUpdateRequest request);

    /** ATT-04 출석 삭제 */
    void delete(Long attendanceId);

    /** ATT-05 출석 통계 */
    AttendanceStatisticsResponse getStatistics(Long classId, Long studentId, LocalDate fromDate, LocalDate toDate);

    Long getMyUserId(String name);

    // ===== 역할별 자기 범위 조회 (본인/자녀만) =====

    /** 학생 본인 출석 이력 (loginId로 본인 student 도출) */
    PageResponse<AttendanceResponse> getMyHistory(String loginId, LocalDate fromDate, LocalDate toDate,
                                                  int page, int size);

    /** 학생 본인 출석 요약 통계 */
    AttendanceStatisticsResponse getMyStatistics(String loginId, LocalDate fromDate, LocalDate toDate);

    /** 학부모 자녀 목록 */
    List<com.edu.domain.attendance.dto.response.ChildOptionResponse> getMyChildren(String loginId);

    /** 학부모 자녀 출석 이력 (자녀 소유 검증) */
    PageResponse<AttendanceResponse> getChildHistory(String loginId, Long studentId,
                                                     LocalDate fromDate, LocalDate toDate, int page, int size);

    /** 학부모 자녀 출석 요약 통계 (자녀 소유 검증) */
    AttendanceStatisticsResponse getChildStatistics(String loginId, Long studentId,
                                                    LocalDate fromDate, LocalDate toDate);

    // ===== 강사 담당반 스코프 (본인 담당반만) =====

    /** 강사 담당반 목록 */
    List<ClassOptionResponse> getMyTeacherClasses(String loginId);

    /** 강사 담당반 한정 이력 (classId 없으면 담당 전체 반) */
    PageResponse<AttendanceResponse> getTeacherHistory(String loginId, Long classId,
                                                       LocalDate fromDate, LocalDate toDate, String keyword,
                                                       int page, int size);

    /** 강사 담당반 한정 통계 */
    AttendanceStatisticsResponse getTeacherStatistics(String loginId, Long classId,
                                                      LocalDate fromDate, LocalDate toDate);

    /** 강사 결석 일괄 처리 (담당반 소유 검증 + 수업 종료 후에만) */
    int markAbsentAsTeacher(String loginId, Long classId, LocalDate date);
}
