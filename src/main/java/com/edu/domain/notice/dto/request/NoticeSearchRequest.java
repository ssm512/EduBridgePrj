package com.edu.domain.notice.dto.request;

import lombok.Data;

/**
 * NOT-02 공지 목록 조회 검색 조건
 * GET /api/notices?targetType=&keyword=&statusCode=&page=&size=
 */
@Data
public class NoticeSearchRequest {

    /** 대상 유형 필터 (ALL/CLASS/STUDENT/PARENT/TEACHER, 없으면 전체) */
    private String targetType;

    /** 제목/내용/작성자명 키워드 검색 */
    private String keyword;

    /**
     * 상태 필터 (ADMIN 전용, 기본 PUBLISHED)
     * ADMIN이 statusCode=DELETED로 소프트 삭제된 공지를 확인할 수 있다.
     */
    private String statusCode;

    /** 페이지 번호 (1부터 시작) */
    private int page = 1;

    /** 페이지 크기 */
    private int size = 10;

    /** LIMIT/OFFSET 계산용 */
    public int getOffset() {
        return (Math.max(page, 1) - 1) * size;
    }
}
