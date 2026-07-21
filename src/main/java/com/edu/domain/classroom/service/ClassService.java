package com.edu.domain.classroom.service;

import com.edu.common.dto.PageResponse;
import com.edu.domain.classroom.dto.ClassCreateRequest;
import com.edu.domain.classroom.dto.ClassDetailResponse;
import com.edu.domain.classroom.dto.ClassResponse;
import com.edu.domain.classroom.dto.ClassSearchRequest;
import com.edu.domain.classroom.dto.ClassUpdateRequest;

/**
 * 반관리 서비스 (CLS-01 ~ CLS-04)
 */
public interface ClassService {

    /** CLS-01 반 등록 */
    ClassResponse createClass(ClassCreateRequest request);

    /** CLS-02 반 목록 조회 (상태/키워드 + 페이징) */
    PageResponse<ClassResponse> getClasses(ClassSearchRequest cond);

    /** CLS-03 반 상세 조회 (수강 학생 목록 포함) */
    ClassDetailResponse getClass(Long classId);

    /** CLS-04 반 수정 */
    ClassResponse updateClass(Long classId, ClassUpdateRequest request);

    /**
     * [추가 2026-07-20] 로그인 강사의 담당반 목록 (teacher/classes 화면, CLS-02와 동일 조건 + teacher_id 한정)
     */
    PageResponse<ClassResponse> getMyClasses(String loginId, ClassSearchRequest cond);

    /**
     * [추가 2026-07-20] 반 상세 조회 - 강사 소유권 검증 포함
     * 요청한 classId가 로그인 강사(loginId)의 담당반이 아니면 403
     */
    ClassDetailResponse getClassForTeacher(Long classId, String loginId);
}
