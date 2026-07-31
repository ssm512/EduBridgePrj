package com.edu.domain.classroom.dto;

import java.util.List;

/**
 * CLS-03 반 상세 응답
 * 반 정보 + 수강 학생 목록
 */
public record ClassDetailResponse(
        ClassResponse classInfo,
        List<ClassStudentDto> students
) {
}
