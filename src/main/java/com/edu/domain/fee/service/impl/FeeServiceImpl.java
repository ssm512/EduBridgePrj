package com.edu.domain.fee.service.impl;

import com.edu.common.dto.PageResponse;
import com.edu.common.exception.ApiException;
import com.edu.domain.fee.dto.request.FeeCreateRequest;
import com.edu.domain.fee.dto.request.FeePaymentRequest;
import com.edu.domain.fee.dto.request.FeeSearchRequest;
import com.edu.domain.fee.dto.request.FeeUpdateRequest;
import com.edu.domain.fee.dto.response.FeeCreateResponse;
import com.edu.domain.fee.dto.response.FeeListResponse;
import com.edu.domain.fee.dto.response.FeePaymentResponse;
import com.edu.domain.fee.dto.response.FeeUpdateResponse;
import com.edu.domain.fee.mapper.FeeMapper;
import com.edu.domain.fee.service.FeeService;
import com.edu.domain.fee.vo.FeePaymentVo;
import com.edu.domain.fee.vo.FeeVo;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

    @Override
    @Transactional
    public FeeUpdateResponse updateFee(Long feeId, FeeUpdateRequest request) {
        FeeVo fee = feeMapper.selectByFeeId(feeId);
        if (fee == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "존재하지 않는 회비입니다");
        }

        long feeAmount = request.getFeeAmount();
        long discountAmount = request.getDiscountAmount() == null ? 0L : request.getDiscountAmount();
        if (discountAmount > feeAmount) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "할인 금액이 청구 금액보다 클 수 없습니다");
        }

        // 이미 납부된 금액보다 청구액을 낮게 수정하는 것은 막는다 (환불 로직 없음)
        // TODO(팀 확인): 명세에 수정 제한 규칙 없음 - 회의 안건
        long paidSum = feeMapper.sumPaidAmountByFeeId(feeId);
        long billableAmount = feeAmount - discountAmount;
        if (paidSum > billableAmount) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "이미 납부된 금액(" + paidSum + "원)보다 청구 금액을 낮게 수정할 수 없습니다. 납부 취소 후 수정해주세요");
        }

        // 금액/기한이 바뀌었으니 상태를 다시 계산해서 함께 저장
        String statusCode = determineStatus(billableAmount, paidSum, request.getDueDate());

        FeeVo updated = FeeVo.builder()
                .feeId(feeId)
                .feeAmount(feeAmount)
                .discountAmount(discountAmount)
                .dueDate(request.getDueDate())
                .description(request.getDescription())
                .statusCode(statusCode)
                .build();

        feeMapper.updateFee(updated);
        return new FeeUpdateResponse(feeId, statusCode);
    }

    @Override
    @Transactional
    public FeePaymentResponse payFee(Long feeId, FeePaymentRequest request) {
        FeeVo fee = feeMapper.selectByFeeId(feeId);
        if (fee == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "존재하지 않는 회비입니다");
        }
        if ("PAID".equals(fee.getStatusCode())) {
            throw new ApiException(HttpStatus.CONFLICT, "이미 납부 완료된 회비입니다");
        }

        // 초과 납부 방지: 유효 납부 합계 + 이번 납부액이 청구액을 넘으면 거절
        long paidSum = feeMapper.sumPaidAmountByFeeId(feeId);
        long billableAmount = fee.getBillableAmount();
        if (paidSum + request.getPaidAmount() > billableAmount) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "납부 금액이 남은 청구 금액(" + (billableAmount - paidSum) + "원)을 초과합니다");
        }

        FeePaymentVo payment = FeePaymentVo.builder()
                .feeId(feeId)
                .paidAmount(request.getPaidAmount())
                .paidAt(request.getPaidAt() == null ? LocalDateTime.now() : request.getPaidAt())
                .paymentMethod(request.getPaymentMethod())
                .build();

        feeMapper.insertPayment(payment);   // useGeneratedKeys 로 payment.paymentId 채워짐

        // 납부 후 상태 재계산 (부분 납부면 PAID 가 아닐 수 있음)
        String statusCode = recalculateFeeStatus(fee);
        return new FeePaymentResponse(payment.getPaymentId(), statusCode);
    }

    @Override
    @Transactional
    public FeePaymentResponse cancelPayment(Long paymentId) {
        FeePaymentVo payment = feeMapper.selectPaymentByPaymentId(paymentId);
        if (payment == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "존재하지 않는 납부 이력입니다");
        }
        if (payment.isCanceled()) {
            throw new ApiException(HttpStatus.CONFLICT, "이미 취소된 납부입니다");
        }

        feeMapper.cancelPayment(paymentId);

        // 취소분이 빠졌으니 회비 상태 재계산 (PAID -> UNPAID/SCHEDULED 로 돌아갈 수 있음)
        FeeVo fee = feeMapper.selectByFeeId(payment.getFeeId());
        String statusCode = recalculateFeeStatus(fee);
        return new FeePaymentResponse(paymentId, statusCode);
    }

    /**
     * 유효 납부 합계를 다시 조회해서 회비 상태를 재계산하고 DB 에 반영한다.
     * 납부 처리(FEE-04)/납부 취소(FEE-05) 공통 로직.
     */
    private String recalculateFeeStatus(FeeVo fee) {
        long paidSum = feeMapper.sumPaidAmountByFeeId(fee.getFeeId());
        String statusCode = determineStatus(fee.getBillableAmount(), paidSum, fee.getDueDate());

        // 현재 상태와 같으면 불필요한 UPDATE 를 생략
        if (!statusCode.equals(fee.getStatusCode())) {
            feeMapper.updateFeeStatus(fee.getFeeId(), statusCode);
        }
        return statusCode;
    }

    /**
     * 상태 결정 규칙 (한 곳에서만 관리):
     * 1. 유효 납부 합계 >= 청구액(할인 반영) -> PAID
     * 2. 미납이면서 기한 경과 -> UNPAID
     * 3. 미납이지만 기한 전 -> SCHEDULED
     */
    private String determineStatus(long billableAmount, long paidSum, LocalDate dueDate) {
        if (paidSum >= billableAmount) {
            return "PAID";
        }
        return dueDate.isBefore(LocalDate.now()) ? "UNPAID" : "SCHEDULED";
    }
}
