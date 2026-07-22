package com.edu.domain.fee.mapper;

import com.edu.domain.fee.dto.request.FeeSearchRequest;
import com.edu.domain.fee.dto.response.FeeListResponse;
import com.edu.domain.fee.dto.response.FeeMonthlyStatResponse;
import com.edu.domain.fee.dto.response.FeeNotificationTargetResponse;
import com.edu.domain.fee.dto.response.FeePaymentHistoryResponse;
import com.edu.domain.fee.vo.FeePaymentVo;
import com.edu.domain.fee.vo.FeeVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface FeeMapper {

    /** 회비 목록 조회 - 검색 조건 + 페이징 (FEE-02) */
    List<FeeListResponse> selectFeeList(FeeSearchRequest search);

    /** 회비 목록 전체 건수 - 페이징 계산용 */
    long countFeeList(FeeSearchRequest search);

    /** 회비 단건 조회 */
    FeeVo selectByFeeId(@Param("feeId") Long feeId);

    /** 회비 등록 (FEE-01) */
    int insertFee(FeeVo fee);

    /** 중복 청구 확인 - 같은 학생/월/반 조합 존재 여부 */
    boolean existsFee(@Param("studentId") Long studentId,
                      @Param("billingMonth") String billingMonth,
                      @Param("classId") Long classId);

    /** 회비 수정 - 금액/할인/기한/비고/상태 (FEE-03) */
    int updateFee(FeeVo fee);

    /** 회비 상태만 변경 - 납부 처리/취소 후 상태 재계산용 */
    int updateFeeStatus(@Param("feeId") Long feeId, @Param("statusCode") String statusCode);

    /** 납부 이력 저장 (FEE-04) */
    int insertPayment(FeePaymentVo payment);

    /** 납부 이력 단건 조회 */
    FeePaymentVo selectPaymentByPaymentId(@Param("paymentId") Long paymentId);

    /** 회비 1건의 납부 이력 목록 조회 - 취소분 포함, 최신순 (FEE-06) */
    List<FeePaymentHistoryResponse> selectPaymentsByFeeId(@Param("feeId") Long feeId);

    /** 유효 납부 합계 - 취소되지 않은 납부 금액 합 */
    long sumPaidAmountByFeeId(@Param("feeId") Long feeId);

    /** 납부 취소 - cancel_yn = 'Y' (FEE-05, 이력은 삭제하지 않는다) */
    int cancelPayment(@Param("paymentId") Long paymentId);

    /** 납부 이력 행 수 - 취소분 포함 (삭제 가능 여부 판단용) */
    long countPaymentsByFeeId(@Param("feeId") Long feeId);

    /** 회비 삭제 - 납부 이력이 전혀 없는 잘못 등록 건만 (명세서 외 추가) */
    int deleteFee(@Param("feeId") Long feeId);

    /** 월 범위 회비 통계 - 청구/납부 합계 + 상태별 건수, billing_month 로 GROUP BY (FEE-06) */
    List<FeeMonthlyStatResponse> selectMonthlyStats(@Param("fromMonth") String fromMonth,
                                                    @Param("toMonth") String toMonth,
                                                    @Param("classId") Long classId);

    /** 회비가 해당 학부모(user_id)의 자녀 것인지 - 학부모 납부 이력 접근 제어 (FEE-14) */
    boolean existsFeeForParent(@Param("feeId") Long feeId, @Param("parentUserId") Long parentUserId);

    /** 회비가 해당 학생 본인(user_id) 것인지 - 학생 납부 이력 접근 제어 (FEE-14 학생 화면 확장) */
    boolean existsFeeForStudent(@Param("feeId") Long feeId, @Param("studentUserId") Long studentUserId);

    /** 회비의 반(class_id)이 해당 강사(user_id)의 담당반인지 - 강사 납부 이력 접근 제어 (2026-07-22 결정) */
    boolean existsFeeForTeacher(@Param("feeId") Long feeId, @Param("teacherUserId") Long teacherUserId);

    /** 납부 이력이 해당 학부모(user_id)의 자녀 것인지 - 영수증 조회 접근 제어 (RCT-01/02) */
    boolean existsPaymentForParent(@Param("paymentId") Long paymentId, @Param("parentUserId") Long parentUserId);

    /** 납부 이력이 해당 학생 본인(user_id) 것인지 - 영수증 조회 접근 제어 (RCT-01/02) */
    boolean existsPaymentForStudent(@Param("paymentId") Long paymentId, @Param("studentUserId") Long studentUserId);

    /** 지정일에 납부 기한이 도래하는 미완납 회비 - 납부 예정 알림 대상 (FEE-08) */
    List<FeeNotificationTargetResponse> selectFeesDueOn(@Param("dueDate") LocalDate dueDate);

    /** 납부 기한이 지난 미완납 회비 - 미납 알림 대상, FEE-07 판정과 동일 조건 (FEE-09) */
    List<FeeNotificationTargetResponse> selectOverdueUnpaidFees();
}
