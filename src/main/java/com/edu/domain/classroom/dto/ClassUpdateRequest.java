package com.edu.domain.classroom.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;

/**
 * CLS-04 반 수정 요청
 * PUT /api/classes/{classId}
 * 명세서 파라미터(className, teacherId, startTime, endTime, statusCode)
 * + 화면 편의상 subject/classroom도 함께 수정 가능
 */
public record ClassUpdateRequest(

        @NotBlank(message = "반 이름은 필수입니다")
        @Size(max = 100, message = "반 이름은 100자 이하여야 합니다")
        String className,

        /** 담당 강사 PK (선택) */
        Long teacherId,

        @Size(max = 100, message = "과목은 100자 이하여야 합니다")
        String subject,

        @Size(max = 100, message = "강의실은 100자 이하여야 합니다")
        String classroom,

        LocalTime startTime,

        LocalTime endTime,

        @NotBlank(message = "상태코드는 필수입니다")
        @Pattern(regexp = "ACTIVE|CLOSED", message = "상태코드는 ACTIVE/CLOSED 중 하나여야 합니다")
        String statusCode
) {
}
