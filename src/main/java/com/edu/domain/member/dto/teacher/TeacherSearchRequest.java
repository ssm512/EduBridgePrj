package com.edu.domain.member.dto.teacher;

import lombok.Data;

/**
 * TEA-02 강사 목록 조회 검색 조건
 * GET /api/teachers?keyword=&page=&size=
 */
@Data
public class TeacherSearchRequest {

    /** 이름/로그인ID/담당과목 키워드 검색 */
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
