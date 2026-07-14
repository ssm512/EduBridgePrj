package com.edu.domain.fee.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 회비 목록 조회 검색 조건 (FEE-02)
 * GET /api/v1/fees?studentId=&billingMonth=&statusCode=&page=&size=
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeeSearchRequest {

    /** 학생 필터 */
    private Long studentId;

    /** 청구 월 필터 (YYYY-MM) */
    private String billingMonth;

    /** 상태 필터 (PAID / UNPAID / SCHEDULED) */
    private String statusCode;

    /** 페이지 번호 (1부터 시작) */
    private int page = 1;

    /** 페이지당 건수 */
    private int size = 10;

    /** LIMIT/OFFSET 계산용 - SQL에서 #{offset}으로 사용 */
    public int getOffset() {
        int safePage = Math.max(page, 1);
        return (safePage - 1) * size;
    }
}
