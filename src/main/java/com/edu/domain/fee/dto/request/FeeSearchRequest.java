package com.edu.domain.fee.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 회비 목록 조회 검색 조건 (FEE-06 목록 / FEE-07 미납 조회)
 * GET /api/v1/fees?studentId=&billingMonth=&statusCode=&overdueOnly=&page=&size=
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

    /**
     * 학부모 회비 조회 스코핑 (FEE-14) - 이 값이 있으면 해당 학부모의 자녀 회비만 조회한다.
     * 보안 필터이므로 클라이언트 입력을 신뢰하지 않고, 컨트롤러가 JWT userId 로 항상 덮어쓴다
     * (직원(ADMIN/TEACHER)은 null 로 두어 전체 조회, PARENT 는 본인 user_id 로 강제).
     */
    private Long parentUserId;

    /**
     * 학생 본인 회비 조회 스코핑 (FEE-14 학생 화면 확장) - 이 값이 있으면 본인 회비만 조회한다.
     * parentUserId 와 동일하게 보안 필터이므로 클라이언트 입력을 신뢰하지 않고,
     * 컨트롤러가 JWT userId 로 항상 덮어쓴다 (STUDENT 요청일 때만 세팅, 그 외 역할은 null).
     */
    private Long studentUserId;

    /**
     * 미납 조회 필터 (FEE-07) - true 면 "납부 기한이 지난 미납 건"만 조회
     * status_code 는 쓰기 이벤트에서만 갱신되어 기한이 지나도 SCHEDULED 로 남는 건이 있으므로,
     * 저장된 상태값이 아니라 due_date + 완납 여부로 직접 판정한다 (SQL 참고).
     * 기본값 false: 미체크 시 기존 목록 동작 그대로.
     */
    private boolean overdueOnly;

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
