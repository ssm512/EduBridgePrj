package com.edu.domain.fee.mapper;

import com.edu.domain.fee.dto.request.FeeSearchRequest;
import com.edu.domain.fee.dto.response.FeeListResponse;
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
}
