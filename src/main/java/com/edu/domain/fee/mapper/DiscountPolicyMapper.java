package com.edu.domain.fee.mapper;

import com.edu.domain.fee.vo.DiscountPolicyVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 할인정책 매퍼 (FEE-10). XML: resources/mapper/fee/DiscountPolicyMapper.xml
 * 삭제는 제공하지 않는다 - 이미 회비에 참조(discount_policy_id FK)된 정책이 있을 수 있어
 * active_yn 토글(update)로 비활성화하는 방식만 지원한다.
 */
@Mapper
public interface DiscountPolicyMapper {

    /** 목록 조회 (활성만 필터 선택, 페이징) */
    List<DiscountPolicyVo> findList(@Param("activeOnly") boolean activeOnly,
                                    @Param("size") int size,
                                    @Param("offset") int offset);

    /** 목록 전체 건수 (페이징용) */
    long countList(@Param("activeOnly") boolean activeOnly);

    /** PK 단건 조회 */
    DiscountPolicyVo findById(@Param("discountPolicyId") Long discountPolicyId);

    /** 등록 (useGeneratedKeys로 discountPolicyId 채움) */
    int insert(DiscountPolicyVo policy);

    /** 수정 */
    int update(DiscountPolicyVo policy);
}