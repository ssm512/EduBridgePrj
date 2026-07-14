package com.edu.domain.member.dto.student;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 학생 응답 DTO (목록/등록/수정 결과용)
 */
public record StudentResponse(
        Long studentId,
        Long userId,
        String loginId,
        String name,
        String email,
        String phone,
        String studentNo,
        LocalDate birthDate,
        String schoolName,
        String gradeLevel,
        String memo,
        String statusCode,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime createdAt
) {
    public static StudentResponse from(StudentDto s) {
        return new StudentResponse(
                s.getStudentId(),
                s.getUserId(),
                s.getLoginId(),
                s.getName(),
                s.getEmail(),
                s.getPhone(),
                s.getStudentNo(),
                s.getBirthDate(),
                s.getSchoolName(),
                s.getGradeLevel(),
                s.getMemo(),
                s.getStatusCode(),
                s.getCreatedAt()
        );
    }
}
