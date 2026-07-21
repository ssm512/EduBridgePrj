package com.edu.domain.fee.mapper;

import com.edu.domain.fee.vo.FeeDiscountVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 회비할인적용 매퍼 (FEE-15~16). XML: resources/mapper/fee/FeeDiscountMapper.xml
 * fee_discounts 는 회비별 할인 적용 이력 테이블 - 확정(DCP-05) 시 한 행씩 쌓인다.
 */
@Mapper
public interface FeeDiscountMapper {

    /** 확정 - 할인 적용 이력 1건 등록 (useGeneratedKeys로 feeDiscountId 채움) */
    int insert(FeeDiscountVo feeDiscount);

    /** 회비(feeId) 기준 적용 이력 전체 조회 - 최신순 */
    List<FeeDiscountVo> findByFeeId(@Param("feeId") Long feeId);

    /** 회비(feeId) 기준 가장 최근 적용 이력 1건 (현재 유효한 할인 스냅샷 확인용) */
    FeeDiscountVo findLatestByFeeId(@Param("feeId") Long feeId);

    /**
     * 회비(feeId) 기준 적용 이력 전체 삭제.
     * fee_discounts.fee_id -> fees.fee_id FK (ON DELETE CASCADE 없음) 때문에,
     * 회비 삭제(FeeService.deleteFee) 전에 반드시 먼저 호출해야 FK 위반이 안 난다.
     */
    int deleteByFeeId(@Param("feeId") Long feeId);
}
