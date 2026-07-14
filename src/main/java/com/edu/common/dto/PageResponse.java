package com.edu.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 공통 페이징 응답 DTO
 * 목록 조회 API에서 도메인 상관없이 재사용한다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {

    /** 현재 페이지 데이터 */
    private List<T> content;

    /** 현재 페이지 번호 (1부터 시작) */
    private int page;

    /** 페이지당 건수 */
    private int size;

    /** 전체 건수 */
    private long totalElements;

    /** 전체 페이지 수 */
    private int totalPages;

    public static <T> PageResponse<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;
        return PageResponse.<T>builder()
                .content(content)
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .build();
    }
}
