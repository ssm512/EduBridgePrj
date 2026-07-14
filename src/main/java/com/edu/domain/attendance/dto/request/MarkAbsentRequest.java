package com.edu.domain.attendance.dto.request;

import java.time.LocalDate;

/**
 * 결석 일괄 처리 요청 (POST /attendance/mark-absent)
 * 해당 반의 그 날짜에 출석 기록이 없는 수강생을 ABSENT로 등록한다.
 */
public record MarkAbsentRequest(
        Long classId,
        LocalDate date
) {
}
