package com.edu.domain.fee.mapper;

import com.edu.domain.fee.vo.DiscountPolicyVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 할인정책 매퍼 (FEE-10). XML: resources/mapper/fee/DiscountPolicyMapper.xml
 * 삭제는 제공하지 않는다 - 이미 회비에 참조(discount_policy_id FK)된 정책이 있을 수 있어
 * active_yn 토글(update)로 비활성화하는 방식만 지원한다.
 */
@Mapper
public interface DiscountPolicyMapper {

    /**
     * 목록 조회 (페이징)
     * @param activeYn   "Y"/"N" 필터, null 이면 전체
     * @param targetDate 기준일 - null 이 아니면 start_date~end_date 범위에 포함되는 정책만 (null 컬럼은 무제한으로 취급)
     */
    List<DiscountPolicyVo> findList(@Param("activeYn") String activeYn,
                                    @Param("targetDate") LocalDate targetDate,
                                    @Param("size") int size,
                                    @Param("offset") int offset);

    /** 목록 전체 건수 (페이징용) - findList와 동일한 필터 조건 */
    long countList(@Param("activeYn") String activeYn, @Param("targetDate") LocalDate targetDate);

    /** PK 단건 조회 */
    DiscountPolicyVo findById(@Param("discountPolicyId") Long discountPolicyId);

    /** 등록 (useGeneratedKeys로 discountPolicyId 채움) */
    int insert(DiscountPolicyVo policy);

    /** 수정 */
    int update(DiscountPolicyVo policy);
}