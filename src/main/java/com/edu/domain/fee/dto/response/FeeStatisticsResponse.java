package com.edu.domain.fee.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 회비 통계 조회 응답 (FEE-06)
 * summary : 선택한 월의 요약 카드용 (총액/납부액/미납액)
 * monthlyStats : 선택한 월 포함 최근 6개월 - 매출 차트 + 상태별 건수 추이 차트 공용
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeeStatisticsResponse {

    /** 조회 기준 월 (YYYY-MM) */
    private String billingMonth;

    /** 반 필터 (없으면 전체) */
    private Long classId;

    /** 기준 월 요약 */
    private FeeMonthlyStatResponse summary;

    /** 최근 6개월 월별 집계 (빈 달은 0 으로 채워짐, 오름차순) */
    private List<FeeMonthlyStatResponse> monthlyStats;
}
