package com.edu.domain.member.service;

import com.edu.common.dto.PageResponse;
import com.edu.domain.member.dto.teacher.TeacherCreateRequest;
import com.edu.domain.member.dto.teacher.TeacherResponse;
import com.edu.domain.member.dto.teacher.TeacherSearchRequest;
import com.edu.domain.member.dto.teacher.TeacherUpdateRequest;

/**
 * 강사관리 서비스 (TEA-01 ~ TEA-03)
 */
public interface TeacherService {

    /** TEA-01 강사 등록 (계정 + 강사 상세) */
    TeacherResponse createTeacher(TeacherCreateRequest request);

    /** TEA-02 강사 목록 조회 (키워드 + 페이징) */
    PageResponse<TeacherResponse> getTeachers(TeacherSearchRequest cond);

    /** TEA-03 강사 수정 (기본정보 + 담당과목/입사일) */
    TeacherResponse updateTeacher(Long teacherId, TeacherUpdateRequest request);
}
