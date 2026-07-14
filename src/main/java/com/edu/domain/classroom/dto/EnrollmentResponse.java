package com.edu.domain.classroom.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 수강 응답 DTO (ENR-01 등록 결과 / 목록 공용)
 */
public record EnrollmentResponse(
        Long enrollmentId,
        Long studentId,
        String studentName,
        String studentNo,
        Long classId,
        String className,
        String subject,
        LocalDate enrollDate,
        LocalDate endDate,
        String statusCode,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime createdAt
) {
    public static EnrollmentResponse from(EnrollmentDto e) {
        return new EnrollmentResponse(
                e.getEnrollmentId(),
                e.getStudentId(),
                e.getStudentName(),
                e.getStudentNo(),
                e.getClassId(),
                e.getClassName(),
                e.getSubject(),
                e.getEnrollDate(),
                e.getEndDate(),
                e.getStatusCode(),
                e.getCreatedAt()
        );
    }
}
