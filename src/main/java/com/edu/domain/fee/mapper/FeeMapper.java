package com.edu.domain.fee.mapper;

import com.edu.domain.fee.dto.request.FeeSearchRequest;
import com.edu.domain.fee.dto.response.FeeListResponse;
import com.edu.domain.fee.dto.response.FeeMonthlyStatResponse;
import com.edu.domain.fee.vo.FeePaymentVo;
import com.edu.domain.fee.vo.FeeVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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
}
