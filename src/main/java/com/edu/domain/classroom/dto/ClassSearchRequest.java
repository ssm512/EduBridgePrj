package com.edu.domain.classroom.dto;

import lombok.Data;

/**
 * CLS-02 반 목록 조회 검색 조건
 * GET /classes?statusCode=&keyword=&page=&size=
 */
@Data
public class ClassSearchRequest {

    /** 운영 상태 필터 (ACTIVE/CLOSED, 없으면 전체) */
    private String statusCode;

    /** 반이름/과목/담당강사명 키워드 검색 */
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
