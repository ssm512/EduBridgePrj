package com.edu.domain.attendance.service.impl;

import com.edu.domain.attendance.dto.request.AttendanceCheckRequest;
import com.edu.domain.attendance.dto.request.AttendanceUpdateRequest;
import com.edu.domain.attendance.dto.request.ManualAttendanceRequest;
import com.edu.domain.attendance.dto.response.AttendanceResponse;
import com.edu.domain.attendance.dto.response.AttendanceStatisticsResponse;
import com.edu.domain.attendance.mapper.AttendanceMapper;
import com.edu.domain.attendance.service.AttendanceService;
import com.edu.domain.attendance.service.AttendanceVerificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 출석 서비스 구현 (뼈대).
 * 매퍼/검증 서비스는 주입해두었고, 실제 흐름은 각 메서드 TODO로 남겨둠.
 */
@Service
@Transactional
public class AttendanceServiceImpl implements AttendanceService {

    private final AttendanceMapper attendanceMapper;
    private final AttendanceVerificationService verificationService;

    public AttendanceServiceImpl(AttendanceMapper attendanceMapper,
                                 AttendanceVerificationService verificationService) {
        this.attendanceMapper = attendanceMapper;
        this.verificationService = verificationService;
    }

    @Override
    public AttendanceResponse checkIn(AttendanceCheckRequest request) {
        // TODO: 1) 중복 출석 확인(countByStudentClassDate)
        //       2) GPS/BLE/RSSI/시간 검증(verificationService)
        //       3) 상태 판정 후 AttendanceRecord 저장(insert, checkType=AUTO)
        //       4) 실패 시 failureReason 저장
        throw new UnsupportedOperationException("TODO: ATT-01 자동 출석 구현");
    }

    @Override
    public AttendanceResponse registerManual(ManualAttendanceRequest request, Long createdBy) {
        // TODO: 수동 출석 저장 (checkType=MANUAL, created_by=createdBy)
        throw new UnsupportedOperationException("TODO: ATT-02 수동 출석 구현");
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceResponse> getHistory(Long studentId, Long classId,
                                               LocalDate fromDate, LocalDate toDate) {
        // TODO: 권한별 조회 범위 제한(본인/자녀 등)은 정책 확정 후 반영
        return attendanceMapper.findList(studentId, classId, fromDate, toDate)
                .stream().map(AttendanceResponse::from).toList();
    }

    @Override
    public AttendanceResponse update(Long attendanceId, AttendanceUpdateRequest request) {
        // TODO: 존재 확인 → updateStatus → 갱신본 반환
        throw new UnsupportedOperationException("TODO: ATT-04 출석 수정 구현");
    }

    @Override
    @Transactional(readOnly = true)
    public AttendanceStatisticsResponse getStatistics(Long classId, Long studentId,
                                                      LocalDate fromDate, LocalDate toDate) {
        // TODO: 상태별 집계 → 출석률 계산
        return AttendanceStatisticsResponse.empty();
    }
}
