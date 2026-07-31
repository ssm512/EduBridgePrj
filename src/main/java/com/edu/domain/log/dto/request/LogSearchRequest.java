package com.edu.domain.log.dto.request;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * 활동로그 검색 조건.
 * GET /api/logs?actionType=&fromDate=&toDate=&page=&size=
 */
@Data
public class LogSearchRequest {

    /** 분류 필터 (action_type). 없으면 전체 */
    private String actionType;

    /** 기간 시작 (created_at::date >=) */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fromDate;

    /** 기간 종료 (created_at::date <=) */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate toDate;

    private int page = 1;
    private int size = 20;

    public int getOffset() {
        return (Math.max(page, 1) - 1) * size;
    }
}
