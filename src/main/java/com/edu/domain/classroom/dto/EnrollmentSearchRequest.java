package com.edu.domain.classroom.dto;

import lombok.Data;

/**
 * 수강 목록 조회 검색 조건 (명세서 외 추가 API - 수강관리 화면용)
 * GET /api/enrollments?classId=&statusCode=&keyword=&page=&size=
 */
@Data
public class EnrollmentSearchRequest {

    /** 반 필터 */
    private Long classId;

    /** 수강 상태 필터 (ACTIVE/ENDED, 없으면 전체) */
    private String statusCode;

    /** 학생 이름/학번 키워드 검색 */
    private String keyword;

    /** 페이지 번호 (1부터 시작) */
    private int page = 1;

    /** 페이지 크기 */
    private int size = 10;

    /** LIMIT/OFFSET 계산용 */
    public int getOffset() {
        return (Math.max(page, 1) - 1) * size;
    }
}
