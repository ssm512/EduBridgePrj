package com.edu.domain.fee.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 회비 목록 조회 응답 (FEE-02)
 * fees + students(users) + classes JOIN 결과 - 화면 SCR-W-11 목록용
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeeListResponse {

    /** 회비 PK */
    private Long feeId;

    /** 학생 PK */
    private Long studentId;

    /** 학생 이름 (users.name) */
    private String studentName;

    /** 학번 (students.student_no) - 동명이인 구분용 */
    private String studentNo;

    /** 생년월일 (students.birth_date) - 동명이인 구분용 */
    private LocalDate birthDate;

    /** 반 PK */
    private Long classId;

    /** 반 이름 (classes.class_name) */
    private String className;

    /** 청구 월 (YYYY-MM) */
    private String billingMonth;

    /** 청구 금액 */
    private Long feeAmount;

    /** 할인 금액 */
    private Long discountAmount;

    /** 적용된 할인정책 PK (선택, 없으면 null - discountAmount 직접입력분) */
    private Long discountPolicyId;

    /** 적용된 할인정책명 (SQL LEFT JOIN discount_policies, 없으면 null) */
    private String discountPolicyName;

    /** 실 청구액 (SQL에서 fee_amount - discount_amount 계산) */
    private Long billableAmount;

    /** 납부 기한 */
    private LocalDate dueDate;

    /** 상태 (PAID / UNPAID / SCHEDULED) */
    private String statusCode;

    /** 비고 */
    private String description;

    /** 가장 최근 유효 납부 이력 PK - 납부 취소 버튼용, 없으면 null (SQL 집계) */
    private Long lastPaymentId;

    /** 유효 납부 합계 (취소분 제외, 없으면 0 - SQL COALESCE) */
    private Long paidSum;

    /** 납부 이력 행 수 (취소분 포함) - 삭제 버튼 활성화 판단용 */
    private Long paymentCount;

    /** 남은 금액 = 실 청구액 - 유효 납부 합계 */
    public Long getRemainingAmount() {
        long billable = billableAmount == null ? 0L : billableAmount;
        long paid = paidSum == null ? 0L : paidSum;
        return billable - paid;
    }
}
