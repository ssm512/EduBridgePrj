package com.edu.domain.attendance.service;

import com.edu.domain.attendance.dto.request.AttendanceCheckRequest;
import com.edu.domain.attendance.dto.request.AttendanceUpdateRequest;
import com.edu.domain.attendance.dto.request.ManualAttendanceRequest;
import com.edu.domain.attendance.dto.response.AttendanceResponse;
import com.edu.domain.attendance.dto.response.AttendanceStatisticsResponse;

import java.time.LocalDate;
import java.util.List;

/**
 * 출석 서비스 (틀). 각 메서드 본문은 담당자가 구현.
 * ATT-01 자동 / ATT-02 수동 / ATT-03 이력 / ATT-04 수정 / ATT-05 통계
 */
public interface AttendanceService {

    /** ATT-01 자동 출석 (GPS/BLE 검증 → 상태 판정 → 저장) */
    AttendanceResponse checkIn(AttendanceCheckRequest request);

    /** ATT-02 수동 출석 등록 */
    AttendanceResponse registerManual(ManualAttendanceRequest request, Long createdBy);

    /** ATT-03 출석 이력 조회 */
    List<AttendanceResponse> getHistory(Long studentId, Long classId, LocalDate fromDate, LocalDate toDate);

    /** ATT-04 출석 수정 */
    AttendanceResponse update(Long attendanceId, AttendanceUpdateRequest request);

    /** ATT-05 출석 통계 */
    AttendanceStatisticsResponse getStatistics(Long classId, Long studentId, LocalDate fromDate, LocalDate toDate);
}
