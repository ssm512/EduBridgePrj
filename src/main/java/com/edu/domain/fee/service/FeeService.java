package com.edu.domain.fee.service;

import com.edu.common.dto.PageResponse;
import com.edu.domain.fee.dto.request.FeeCreateRequest;
import com.edu.domain.fee.dto.request.FeePaymentRequest;
import com.edu.domain.fee.dto.request.FeeSearchRequest;
import com.edu.domain.fee.dto.request.FeeUpdateRequest;
import com.edu.domain.fee.dto.response.FeeCreateResponse;
import com.edu.domain.fee.dto.response.FeeDiscountResponse;
import com.edu.domain.fee.dto.response.FeeListResponse;
import com.edu.domain.fee.dto.response.FeeNotificationTargetResponse;
import com.edu.domain.fee.dto.response.FeePaymentHistoryResponse;
import com.edu.domain.fee.dto.response.FeePaymentResponse;
import com.edu.domain.fee.dto.response.FeeStatisticsResponse;
import com.edu.domain.fee.dto.response.FeeUpdateResponse;
import com.edu.domain.fee.dto.response.PaymentReceiptResponse;

import java.time.LocalDate;
import java.util.List;

public interface FeeService {

    /** 회비 목록 조회 - 검색 조건 + 페이징 (FEE-02) */
    PageResponse<FeeListResponse> getFeeList(FeeSearchRequest search);

    /**
     * 회비 등록 (FEE-01)
     * currentUserId: 로그인한 관리자 PK. discountPolicyId 로 할인을 적용하면 fee_discounts.applied_by 로 기록된다
     * (FEE-15/16, DCP-05). discountPolicyId 가 없으면(직접입력/무할인) 이력을 남기지 않으므로 사용되지 않는다.
     */
    FeeCreateResponse createFee(FeeCreateRequest request, Long currentUserId);

    /** 회비 수정 - 금액/할인/기한/비고 (FEE-03). currentUserId 용도는 createFee 와 동일 */
    FeeUpdateResponse updateFee(Long feeId, FeeUpdateRequest request, Long currentUserId);

    /**
     * 납부 처리 - 이력 저장 + 상태 재계산 (FEE-04)
     * issuedByUserId: 로그인한 사용자 PK. 납부 처리 직후 영수증을 자동 발급(FEE-19)하면서
     * payment_receipts.issued_by 로 기록된다 (명세에 별도 발급 API 가 없어 payFee 내부에서 처리).
     */
    FeePaymentResponse payFee(Long feeId, FeePaymentRequest request, Long issuedByUserId);

    /**
     * 납부 취소 - cancel_yn 처리 + 상태 재계산 (FEE-05)
     * 연결된 영수증이 ISSUED 상태면 함께 CANCELLED 로 전환한다 (FEE-20, 고정 문구 사유 사용).
     */
    FeePaymentResponse cancelPayment(Long paymentId);

    /**
     * 영수증 상세 조회 (RCT-01/02) - ADMIN/PARENT/STUDENT 만 대상 (TEACHER 는 2026-07-22 재논의로 접근 철회).
     * parentUserId != null 이면 학부모 스코핑, studentUserId != null 이면 학생 본인 스코핑.
     * ADMIN 은 둘 다 null 로 넘겨 제한 없이 조회한다.
     * 납부 이력 자체가 없으면 404, 있어도 영수증이 없으면(이 기능 배포 전 이력) 404.
     */
    PaymentReceiptResponse getReceipt(Long paymentId, Long parentUserId, Long studentUserId);

    /**
     * 납부 이력 조회 - 회비 1건의 납부 기록 목록, 취소분 포함 (FEE-06)
     * parentUserId != null 이면 학부모 스코핑(FEE-14): 본인 자녀 회비가 아니면 403.
     * studentUserId != null 이면 학생 본인 스코핑(FEE-14 학생 화면 확장): 본인 회비가 아니면 403.
     * teacherUserId != null 이면 담당 강사 스코핑(2026-07-22 결정): 본인 담당반의 회비가 아니면 403.
     * ADMIN 은 셋 다 null 로 넘겨 제한 없이 조회한다. 세 값은 동시에 값을 갖지 않는다
     * (컨트롤러가 로그인 역할 하나에 대해서만 스코핑 값을 채운다).
     */
    List<FeePaymentHistoryResponse> getPaymentHistory(Long feeId, Long parentUserId, Long studentUserId, Long teacherUserId);

    /**
     * 할인 적용 이력 조회 - 회비 1건에 적용된 할인정책 이력 목록, 최신순 (FEE-15/16, DCP-05)
     * 스코핑 규칙은 getPaymentHistory 와 동일 (parentUserId/studentUserId/teacherUserId 는 동시에 값을 갖지 않음).
     */
    List<FeeDiscountResponse> getDiscountHistory(Long feeId, Long parentUserId, Long studentUserId, Long teacherUserId);

    /** 회비 통계 - 기준 월 요약 + 최근 6개월 추이 (FEE-06) */
    FeeStatisticsResponse getStatistics(String billingMonth, Long classId);

    /** 회비 삭제 - 납부 이력이 전혀 없는 잘못 등록 건만 (명세서 외 추가) */
    void deleteFee(Long feeId);

    /** 지정일에 납부 기한이 도래하는 미완납 회비 목록 - 납부 예정 알림 배치용 (FEE-08) */
    List<FeeNotificationTargetResponse> getFeesDueOn(LocalDate dueDate);

    /** 납부 기한이 지난 미완납 회비 목록 - 미납 알림 배치용 (FEE-09) */
    List<FeeNotificationTargetResponse> getOverdueUnpaidFees();
}
