package com.edu.domain.member.dto.teacher;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 강사 응답 DTO
 * TEA-01 등록 결과 / TEA-02 목록 / TEA-03 수정 결과 공용
 */
public record TeacherResponse(
        Long teacherId,
        Long userId,
        String loginId,
        String name,
        String email,
        String phone,
        String subject,
        LocalDate hireDate,
        String statusCode,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime createdAt
) {
    public static TeacherResponse from(TeacherDto t) {
        return new TeacherResponse(
                t.getTeacherId(),
                t.getUserId(),
                t.getLoginId(),
                t.getName(),
                t.getEmail(),
                t.getPhone(),
                t.getSubject(),
                t.getHireDate(),
                t.getStatusCode(),
                t.getCreatedAt()
        );
    }
}
