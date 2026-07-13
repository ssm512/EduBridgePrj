package com.edu.domain.member.dto.student;

import java.util.List;

/**
 * STU-03 학생 상세 응답
 * 학생 기본정보 + 보호자 목록 + 수강반 목록
 */
public record StudentDetailResponse(
        StudentResponse student,
        List<GuardianDto> guardians,
        List<StudentEnrollmentDto> enrollments
) {
}
