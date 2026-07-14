package com.edu.domain.fee.service.impl;

import com.edu.common.dto.PageResponse;
import com.edu.domain.fee.dto.request.FeeSearchRequest;
import com.edu.domain.fee.dto.response.FeeListResponse;
import com.edu.domain.fee.mapper.FeeMapper;
import com.edu.domain.fee.service.FeeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class FeeServiceImpl implements FeeService {

    private final FeeMapper feeMapper;

    public FeeServiceImpl(FeeMapper feeMapper) {
        this.feeMapper = feeMapper;
    }

    @Override
    public PageResponse<FeeListResponse> getFeeList(FeeSearchRequest search) {
        long totalElements = feeMapper.countFeeList(search);

        // 건수가 0이면 목록 쿼리는 실행할 필요 없음
        if (totalElements == 0) {
            return PageResponse.of(List.of(), search.getPage(), search.getSize(), 0);
        }

        List<FeeListResponse> content = feeMapper.selectFeeList(search);
        return PageResponse.of(content, search.getPage(), search.getSize(), totalElements);
    }
}
