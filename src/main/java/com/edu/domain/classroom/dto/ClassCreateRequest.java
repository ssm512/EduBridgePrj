package com.edu.domain.classroom.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;

/**
 * CLS-01 반 등록 요청
 * POST /api/classes
 */
public record ClassCreateRequest(

        @NotBlank(message = "반 이름은 필수입니다")
        @Size(max = 100, message = "반 이름은 100자 이하여야 합니다")
        String className,

        /** 담당 강사 PK (선택, 지정 시 존재 검증) */
        Long teacherId,

        @Size(max = 100, message = "과목은 100자 이하여야 합니다")
        String subject,

        @Size(max = 100, message = "강의실은 100자 이하여야 합니다")
        String classroom,

        /** 수업 시작 시간 (HH:mm) */
        LocalTime startTime,

        /** 수업 종료 시간 (HH:mm) */
        LocalTime endTime
) {
}
