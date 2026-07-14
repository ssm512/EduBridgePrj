package com.edu.domain.classroom.service;

import com.edu.common.dto.PageResponse;
import com.edu.domain.classroom.dto.EnrollmentCreateRequest;
import com.edu.domain.classroom.dto.EnrollmentEndRequest;
import com.edu.domain.classroom.dto.EnrollmentResponse;
import com.edu.domain.classroom.dto.EnrollmentSearchRequest;

/**
 * 수강관리 서비스 (ENR-01 ~ ENR-02)
 */
public interface EnrollmentService {

    /** ENR-01 수강 등록 (학생을 특정 반에 등록) */
    EnrollmentResponse enroll(EnrollmentCreateRequest request);

    /** 수강 목록 조회 (명세서 외 - 수강관리 화면용) */
    PageResponse<EnrollmentResponse> getEnrollments(EnrollmentSearchRequest cond);

    /** ENR-02 수강 해제 (ENDED + 종료일 기록) */
    EnrollmentResponse endEnrollment(Long enrollmentId, EnrollmentEndRequest request);
}
