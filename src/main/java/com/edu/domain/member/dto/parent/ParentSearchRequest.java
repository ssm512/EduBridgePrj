package com.edu.domain.member.dto.parent;

import lombok.Data;

/**
 * PAR-02 학부모 목록 조회 검색 조건
 * GET /api/parents?keyword=&page=&size=
 */
@Data
public class ParentSearchRequest {

    /** 이름/로그인ID/연락처 키워드 검색 */
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
