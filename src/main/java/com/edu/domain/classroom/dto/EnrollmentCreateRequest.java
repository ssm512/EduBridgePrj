package com.edu.domain.classroom.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * ENR-01 수강 등록 요청
 * POST /api/enrollments
 */
public record EnrollmentCreateRequest(

        @NotNull(message = "studentId는 필수입니다")
        Long studentId,

        @NotNull(message = "classId는 필수입니다")
        Long classId,

        /** 수강 시작일 (생략 시 오늘) */
        LocalDate enrollDate
) {
}
