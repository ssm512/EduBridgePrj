package com.edu.domain.member.dto.student;

import jakarta.validation.constraints.Size;

/**
 * STU-04 학생 수정 요청
 * PUT /api/students/{studentId}
 * 명세서 파라미터: studentNo, schoolName, gradeLevel, memo
 */
public record StudentUpdateRequest(

        @Size(max = 30, message = "학번은 30자 이하여야 합니다")
        String studentNo,

        @Size(max = 100, message = "학교명은 100자 이하여야 합니다")
        String schoolName,

        @Size(max = 50, message = "학년은 50자 이하여야 합니다")
        String gradeLevel,

        String memo
) {
}
