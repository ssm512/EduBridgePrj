package com.edu.domain.fee.service.impl;

import com.edu.common.dto.PageResponse;
import com.edu.common.exception.ApiException;
import com.edu.domain.fee.dto.request.FeeCreateRequest;
import com.edu.domain.fee.dto.request.FeeSearchRequest;
import com.edu.domain.fee.dto.response.FeeCreateResponse;
import com.edu.domain.fee.dto.response.FeeListResponse;
import com.edu.domain.fee.mapper.FeeMapper;
import com.edu.domain.fee.service.FeeService;
import com.edu.domain.fee.vo.FeeVo;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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

    @Override
    @Transactional  // 클래스 레벨 readOnly 를 쓰기 트랜잭션으로 덮어쓴다
    public FeeCreateResponse createFee(FeeCreateRequest request) {
        long feeAmount = request.getFeeAmount();
        long discountAmount = request.getDiscountAmount() == null ? 0L : request.getDiscountAmount();

        // 필드 단위 검증(@Valid)으로 못 잡는 필드 간 규칙은 서비스에서 검증
        if (discountAmount > feeAmount) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "할인 금액이 청구 금액보다 클 수 없습니다");
        }

        // 같은 학생/월/반 중복 청구 방지
        if (feeMapper.existsFee(request.getStudentId(), request.getBillingMonth(), request.getClassId())) {
            throw new ApiException(HttpStatus.CONFLICT, "해당 학생의 같은 월/반 회비가 이미 등록되어 있습니다");
        }

        // 초기 상태: 납부 기한이 지나지 않았으면 SCHEDULED, 이미 지났으면 UNPAID
        // TODO(팀 확인): 명세에 초기 상태 결정 규칙 없음 - 회의 안건
        String statusCode = request.getDueDate().isBefore(LocalDate.now()) ? "UNPAID" : "SCHEDULED";

        FeeVo fee = FeeVo.builder()
                .studentId(request.getStudentId())
                .classId(request.getClassId())
                .billingMonth(request.getBillingMonth())
                .feeAmount(feeAmount)
                .discountAmount(discountAmount)
                .dueDate(request.getDueDate())
                .statusCode(statusCode)
                .description(request.getDescription())
                .build();

        feeMapper.insertFee(fee);   // useGeneratedKeys 로 fee.feeId 채워짐
        return new FeeCreateResponse(fee.getFeeId(), statusCode);
    }
}
