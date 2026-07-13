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
}
