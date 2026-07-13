package com.edu.domain.member.dto;

import lombok.Data;

/**
 * USER-01 회원 목록 조회 검색 조건
 * GET /api/users?roleCode=&statusCode=&keyword=&page=&size=
 */
@Data
public class UserSearchRequest {

    /** 권한 필터 (ADMIN/TEACHER/STUDENT/PARENT, 없으면 전체) */
    private String roleCode;

    /** 상태 필터 (ACTIVE/INACTIVE/WITHDRAWN, 없으면 전체) */
    private String statusCode;

    /** 이름/로그인ID/이메일 키워드 검색 */
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
