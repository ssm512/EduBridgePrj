package com.edu.common.dto;

import java.util.List;

/**
 * 목록 조회 공통 페이징 응답
 *
 * @param items      현재 페이지 데이터 목록
 * @param page       현재 페이지 번호 (1부터 시작)
 * @param size       페이지 크기
 * @param totalCount 전체 건수
 * @param totalPages 전체 페이지 수
 */
public record PageResponse<T>(
        List<T> items,
        int page,
        int size,
        long totalCount,
        int totalPages
) {
    public static <T> PageResponse<T> of(List<T> items, int page, int size, long totalCount) {
        int totalPages = size > 0 ? (int) Math.ceil((double) totalCount / size) : 0;
        return new PageResponse<>(items, page, size, totalCount, totalPages);
    }
}
