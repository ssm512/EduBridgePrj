package com.edu.domain.classroom.dto;

import java.time.LocalDate;

/**
 * ENR-02 수강 해제 요청
 * PUT /api/enrollments/{enrollmentId}/end
 * DELETE가 아니라 상태(ENDED) + 종료일 기록 방식
 */
public record EnrollmentEndRequest(

        /** 수강 종료일 (생략 시 오늘) */
        LocalDate endDate
) {
}
