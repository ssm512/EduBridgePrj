package com.edu.domain.member.service;

import com.edu.common.dto.PageResponse;
import com.edu.domain.member.dto.student.StudentCreateRequest;
import com.edu.domain.member.dto.student.StudentDetailResponse;
import com.edu.domain.member.dto.student.StudentResponse;
import com.edu.domain.member.dto.student.StudentSearchRequest;
import com.edu.domain.member.dto.student.StudentUpdateRequest;
import org.springframework.security.core.Authentication;

/**
 * 학생관리 서비스 (STU-01 ~ STU-04)
 */
public interface StudentService {

    /** STU-01 학생 등록 (계정 + 학생 상세) */
    StudentResponse createStudent(StudentCreateRequest request);

    /** STU-02 학생 목록 조회 (반/키워드 + 페이징) */
    PageResponse<StudentResponse> getStudents(StudentSearchRequest cond);

    /**
     * STU-03 학생 상세 조회 (보호자/수강반 포함)
     * STUDENT는 본인, PARENT는 자기 자녀만 조회 가능 → Authentication으로 검증
     */
    StudentDetailResponse getStudent(Long studentId, Authentication authentication);

    /** STU-04 학생 수정 (학번/학교/학년/메모) */
    StudentResponse updateStudent(Long studentId, StudentUpdateRequest request);
}
