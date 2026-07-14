package com.edu.domain.member.dto.student;

import lombok.Data;

/**
 * STU-02 학생 목록 조회 검색 조건
 * GET /api/students?classId=&keyword=&page=&size=
 */
@Data
public class StudentSearchRequest {

    /** 수강 반 필터 (해당 반에 ACTIVE 수강 중인 학생) */
    private Long classId;

    /** 이름/로그인ID/학번 키워드 검색 */
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
