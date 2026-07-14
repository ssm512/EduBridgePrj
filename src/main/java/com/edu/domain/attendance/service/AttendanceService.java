package com.edu.domain.attendance.service;

import com.edu.domain.attendance.dto.request.AttendanceCheckRequest;
import com.edu.domain.attendance.dto.request.AttendanceCheckoutRequest;
import com.edu.domain.attendance.dto.request.AttendanceUpdateRequest;
import com.edu.domain.attendance.dto.request.ManualAttendanceRequest;
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

    /** ATT-08 결석 일괄 처리: 해당 반의 그 날짜 미기록 수강생을 ABSENT로 등록. 등록 건수 반환 */
    int markAbsent(Long classId, LocalDate date);

    /** ATT-02 수동 출석 등록 */
    AttendanceResponse registerManual(ManualAttendanceRequest request, Long createdBy);

    /** ATT-03 출석 이력 조회 (keyword = 학생 이름 부분 일치) */
    List<AttendanceResponse> getHistory(Long studentId, Long classId,
                                        LocalDate fromDate, LocalDate toDate, String keyword);

    /** ATT-04 출석 수정 */
    AttendanceResponse update(Long attendanceId, AttendanceUpdateRequest request);

    /** ATT-04 출석 삭제 */
    void delete(Long attendanceId);

    /** ATT-05 출석 통계 */
    AttendanceStatisticsResponse getStatistics(Long classId, Long studentId, LocalDate fromDate, LocalDate toDate);
}
