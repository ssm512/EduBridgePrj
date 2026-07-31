package com.edu.domain.fee.service.impl;

import com.edu.common.dto.PageResponse;
import com.edu.common.exception.ApiException;
import com.edu.domain.fee.dto.request.FeeCreateRequest;
import com.edu.domain.fee.dto.request.FeePaymentRequest;
import com.edu.domain.fee.dto.request.FeeSearchRequest;
import com.edu.domain.fee.dto.request.FeeUpdateRequest;
import com.edu.domain.fee.dto.response.FeeCreateResponse;
import com.edu.domain.fee.dto.response.FeeListResponse;
import com.edu.domain.fee.dto.response.FeeMonthlyStatResponse;
import com.edu.domain.fee.dto.response.FeeNotificationTargetResponse;
import com.edu.domain.fee.dto.response.FeePaymentHistoryResponse;
import com.edu.domain.fee.dto.response.FeePaymentResponse;
import com.edu.domain.fee.dto.response.FeeStatisticsResponse;
import com.edu.domain.fee.dto.response.FeeDiscountResponse;
import com.edu.domain.fee.dto.response.FeeUpdateResponse;
import com.edu.domain.fee.dto.response.PaymentReceiptResponse;
import com.edu.domain.fee.mapper.FeeDiscountMapper;
import com.edu.domain.fee.mapper.FeeMapper;
import com.edu.domain.fee.mapper.PaymentReceiptMapper;
import com.edu.domain.fee.service.DiscountPolicyService;
import com.edu.domain.fee.service.FeeService;
import com.edu.domain.fee.vo.FeeDiscountVo;
import com.edu.domain.fee.vo.FeePaymentVo;
import com.edu.domain.fee.vo.FeeVo;
import com.edu.domain.fee.vo.PaymentReceiptVo;
import com.edu.domain.notification.service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class FeeServiceImpl implements FeeService {

    /** FEE-20: 납부 취소 시 영수증에 남기는 취소 사유 - 명세의 cancelReason 파라미터를 payFee 쪽에서 받지 않기로 한
     * 기존 결정(FeePaymentApiController 참고)을 유지하기 위해 고정 문구를 쓴다 (2026-07-22 결정) */
    private static final String RECEIPT_CANCEL_REASON = "납부 취소에 따른 자동 취소";

    private final FeeMapper feeMapper;
    private final NotificationService notificationService;
    private final DiscountPolicyService discountPolicyService;
    private final FeeDiscountMapper feeDiscountMapper;
    private final PaymentReceiptMapper paymentReceiptMapper;

    public FeeServiceImpl(FeeMapper feeMapper, NotificationService notificationService,
                          DiscountPolicyService discountPolicyService, FeeDiscountMapper feeDiscountMapper,
                          PaymentReceiptMapper paymentReceiptMapper) {
        this.feeMapper = feeMapper;
        this.notificationService = notificationService;
        this.discountPolicyService = discountPolicyService;
        this.feeDiscountMapper = feeDiscountMapper;
        this.paymentReceiptMapper = paymentReceiptMapper;
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
    public FeeCreateResponse createFee(FeeCreateRequest request, Long currentUserId) {
        long feeAmount = request.getFeeAmount();
        // FEE-10: discountPolicyId 가 있으면 정책 기준 자동계산이 discountAmount 직접입력을 대체한다
        long discountAmount = resolveDiscountAmount(request.getDiscountPolicyId(), request.getDiscountAmount(), feeAmount);

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
                .discountPolicyId(request.getDiscountPolicyId())
                .dueDate(request.getDueDate())
                .statusCode(statusCode)
                .description(request.getDescription())
                .build();

        feeMapper.insertFee(fee);   // useGeneratedKeys 로 fee.feeId 채워짐

        // FEE-15/16(DCP-05): 정책으로 계산된 할인이면 fee_discounts 에 이력 1건을 남긴다
        recordDiscountHistory(fee.getFeeId(), request.getDiscountPolicyId(), discountAmount, currentUserId);

        // 도메인 연동: 회비 등록 시 학생의 학부모에게 납부 안내 알림 생성
        // 알림 생성이 실패하면 회비 등록도 롤백된다 (같은 트랜잭션)
        // TODO(팀 확인): 알림 실패 시 회비 등록까지 취소할지, 분리할지 - 회의 안건
        notificationService.notifyParentsOfStudent(
                request.getStudentId(),
                "FEE",
                request.getBillingMonth() + " 회비 납부 안내",
                request.getBillingMonth() + " 회비 " + fee.getBillableAmount() + "원이 청구되었습니다. 납부 기한: " + request.getDueDate()
        );

        return new FeeCreateResponse(fee.getFeeId(), statusCode);
    }

    @Override
    @Transactional
    public FeeUpdateResponse updateFee(Long feeId, FeeUpdateRequest request, Long currentUserId) {
        FeeVo fee = feeMapper.selectByFeeId(feeId);
        if (fee == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "존재하지 않는 회비입니다");
        }

        long feeAmount = request.getFeeAmount();
        // FEE-10: discountPolicyId 가 있으면 정책 기준 자동계산이 discountAmount 직접입력을 대체한다
        long discountAmount = resolveDiscountAmount(request.getDiscountPolicyId(), request.getDiscountAmount(), feeAmount);
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
                .discountPolicyId(request.getDiscountPolicyId())
                .dueDate(request.getDueDate())
                .description(request.getDescription())
                .statusCode(statusCode)
                .build();

        feeMapper.updateFee(updated);

        // FEE-15/16(DCP-05): 수정 시에도 정책 기준 할인이면 이력을 새로 한 행 남긴다
        // (재적용/정정 이력을 남기기 위해 매번 새 행 - VO/XML 주석과 동일한 설계)
        recordDiscountHistory(feeId, request.getDiscountPolicyId(), discountAmount, currentUserId);

        return new FeeUpdateResponse(feeId, statusCode);
    }

    @Override
    @Transactional
    public FeePaymentResponse payFee(Long feeId, FeePaymentRequest request, Long issuedByUserId) {
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

        // FEE-19: 납부 처리 직후 영수증을 자동 발급한다. 명세에 별도 발급(POST) API 가 없어 여기서 처리.
        // 영수증 번호는 payment_id 가 이미 DB UNIQUE 라 이 값을 그대로 zero-pad 하면 중복 방지가 구조적으로 보장된다.
        String receiptNo = String.format("RCP%08d", payment.getPaymentId());
        PaymentReceiptVo receipt = PaymentReceiptVo.builder()
                .paymentId(payment.getPaymentId())
                .receiptNo(receiptNo)
                .issuedBy(issuedByUserId)
                .build();
        paymentReceiptMapper.insert(receipt);

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

        // FEE-20: 연결된 영수증도 함께 취소 처리. status_code='ISSUED' 조건 덕분에 영수증이 없거나
        // 이미 취소된 경우엔 0건 UPDATE 로 조용히 끝난다(에러 아님) - 이 기능 배포 전 이력 방어.
        paymentReceiptMapper.cancelByPaymentId(paymentId, RECEIPT_CANCEL_REASON);

        // 취소분이 빠졌으니 회비 상태 재계산 (PAID -> UNPAID/SCHEDULED 로 돌아갈 수 있음)
        FeeVo fee = feeMapper.selectByFeeId(payment.getFeeId());
        String statusCode = recalculateFeeStatus(fee);
        return new FeePaymentResponse(paymentId, statusCode);
    }

    @Override
    public PaymentReceiptResponse getReceipt(Long paymentId, Long parentUserId, Long studentUserId) {
        // 존재하지 않는 납부 이력이면 404 - 스코핑 체크보다 먼저 확인해 케이스를 명확히 구분
        if (feeMapper.selectPaymentByPaymentId(paymentId) == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "존재하지 않는 납부 이력입니다");
        }
        // getPaymentHistory 와 동일한 스코핑 규칙 (403으로 응답해 존재 여부 노출 최소화)
        // TEACHER 는 이 API 자체를 호출할 권한이 없으므로(컨트롤러 @PreAuthorize) teacher 스코핑 분기가 필요 없다.
        if (parentUserId != null && !feeMapper.existsPaymentForParent(paymentId, parentUserId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "본인 자녀의 영수증만 조회할 수 있습니다");
        }
        if (studentUserId != null && !feeMapper.existsPaymentForStudent(paymentId, studentUserId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "본인 영수증만 조회할 수 있습니다");
        }
        // 이 기능 배포 전 납부 이력이면 영수증이 없을 수 있음 - 404
        PaymentReceiptResponse receipt = paymentReceiptMapper.selectDetailByPaymentId(paymentId);
        if (receipt == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "발급된 영수증이 없습니다");
        }
        return receipt;
    }

    @Override
    public List<FeePaymentHistoryResponse> getPaymentHistory(Long feeId, Long parentUserId, Long studentUserId, Long teacherUserId) {
        // 존재하지 않는 회비면 404 - 빈 목록과 "잘못된 회비"를 구분하기 위함
        if (feeMapper.selectByFeeId(feeId) == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "존재하지 않는 회비입니다");
        }
        // FEE-14 학부모 스코핑: parentUserId 가 있으면(=PARENT 요청) 본인 자녀 회비인지 확인.
        // 404 가 아니라 403 으로 응답해 "회비 존재 여부" 정보 노출을 최소화한다.
        if (parentUserId != null && !feeMapper.existsFeeForParent(feeId, parentUserId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "본인 자녀의 회비만 조회할 수 있습니다");
        }
        // FEE-14 학생 스코핑(학생 화면 확장): studentUserId 가 있으면(=STUDENT 요청) 본인 회비인지 확인.
        // 마찬가지로 403 으로 응답해 "회비 존재 여부" 정보 노출을 최소화한다.
        if (studentUserId != null && !feeMapper.existsFeeForStudent(feeId, studentUserId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "본인 회비만 조회할 수 있습니다");
        }
        // 담당 강사 스코핑 (2026-07-22 결정): teacherUserId 가 있으면(=TEACHER 요청) 본인 담당반 회비인지 확인.
        if (teacherUserId != null && !feeMapper.existsFeeForTeacher(feeId, teacherUserId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "본인 담당반의 회비만 조회할 수 있습니다");
        }
        return feeMapper.selectPaymentsByFeeId(feeId);
    }

    @Override
    public List<FeeDiscountResponse> getDiscountHistory(Long feeId, Long parentUserId, Long studentUserId, Long teacherUserId) {
        // 존재하지 않는 회비면 404 - getPaymentHistory 와 동일한 이유(빈 목록과 구분)
        if (feeMapper.selectByFeeId(feeId) == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "존재하지 않는 회비입니다");
        }
        // FEE-14 스코핑: getPaymentHistory 와 동일한 규칙 재사용 (본인 자녀/본인 회비가 아니면 403)
        if (parentUserId != null && !feeMapper.existsFeeForParent(feeId, parentUserId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "본인 자녀의 회비만 조회할 수 있습니다");
        }
        if (studentUserId != null && !feeMapper.existsFeeForStudent(feeId, studentUserId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "본인 회비만 조회할 수 있습니다");
        }
        // 담당 강사 스코핑 (2026-07-22 결정): getPaymentHistory 와 동일한 규칙 재사용
        if (teacherUserId != null && !feeMapper.existsFeeForTeacher(feeId, teacherUserId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "본인 담당반의 회비만 조회할 수 있습니다");
        }
        return feeDiscountMapper.findByFeeId(feeId).stream()
                .map(v -> FeeDiscountResponse.builder()
                        .feeDiscountId(v.getFeeDiscountId())
                        .feeId(v.getFeeId())
                        .discountPolicyId(v.getDiscountPolicyId())
                        .discountAmount(v.getDiscountAmount())
                        .reason(v.getReason())
                        .appliedBy(v.getAppliedBy())
                        .createdAt(v.getCreatedAt())
                        .build())
                .toList();
    }

    @Override
    @Transactional
    public void deleteFee(Long feeId) {
        FeeVo fee = feeMapper.selectByFeeId(feeId);
        if (fee == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "존재하지 않는 회비입니다");
        }

        // 납부 이력(취소분 포함)이 있으면 삭제 불가 - 돈이 오갔던 기록의 근거를 지울 수 없다.
        // 서비스 검증이 1차 방어, fee_payments FK 제약이 2차(최종) 방어
        long paymentCount = feeMapper.countPaymentsByFeeId(feeId);
        if (paymentCount > 0) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "납부 이력이 " + paymentCount + "건 있는 회비는 삭제할 수 없습니다. 잘못 등록된 건만 삭제 가능합니다");
        }

        // fee_discounts.fee_id -> fees.fee_id FK (ON DELETE CASCADE 없음) 방어:
        // 할인정책 적용 이력이 남아있으면 회비 삭제 시 FK 위반(500)이 나므로 먼저 지운다.
        // fee_discounts 는 fee_payments 와 달리 "돈이 오간 기록"이 아니라 시스템이 남긴 계산 스냅샷이라
        // 삭제를 막을 이유 없이 함께 지워도 된다고 판단함.
        feeDiscountMapper.deleteByFeeId(feeId);

        feeMapper.deleteFee(feeId);
    }

    @Override
    public List<FeeNotificationTargetResponse> getFeesDueOn(LocalDate dueDate) {
        return feeMapper.selectFeesDueOn(dueDate);
    }

    @Override
    public List<FeeNotificationTargetResponse> getOverdueUnpaidFees() {
        return feeMapper.selectOverdueUnpaidFees();
    }

    /** 통계 추이 차트에 보여줄 개월 수 (기준 월 포함) */
    private static final int STATS_MONTH_RANGE = 6;

    @Override
    public FeeStatisticsResponse getStatistics(String billingMonth, Long classId) {
        // 기준 월: 미지정이면 이번 달, 형식이 틀리면 400
        YearMonth targetMonth;
        try {
            targetMonth = (billingMonth == null || billingMonth.isBlank())
                    ? YearMonth.now()
                    : YearMonth.parse(billingMonth);   // "YYYY-MM" 형식 그대로 파싱됨
        } catch (DateTimeParseException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "조회 월은 YYYY-MM 형식이어야 합니다");
        }

        // 기준 월 포함 최근 6개월 범위 (billing_month 는 문자열이라 toString 결과로 비교)
        YearMonth fromMonth = targetMonth.minusMonths(STATS_MONTH_RANGE - 1);
        List<FeeMonthlyStatResponse> stats =
                feeMapper.selectMonthlyStats(fromMonth.toString(), targetMonth.toString(), classId);

        // 데이터 없는 달은 SQL 결과에 행이 없으므로 0 값으로 채운다 (차트 X축이 끊기지 않게)
        Map<String, FeeMonthlyStatResponse> statsByMonth = stats.stream()
                .collect(Collectors.toMap(FeeMonthlyStatResponse::getBillingMonth, Function.identity()));

        List<FeeMonthlyStatResponse> monthlyStats = new ArrayList<>();
        for (YearMonth m = fromMonth; !m.isAfter(targetMonth); m = m.plusMonths(1)) {
            String key = m.toString();
            monthlyStats.add(statsByMonth.getOrDefault(key, FeeMonthlyStatResponse.empty(key)));
        }

        // 요약 카드 = 범위의 마지막 요소(기준 월)
        FeeMonthlyStatResponse summary = monthlyStats.get(monthlyStats.size() - 1);

        return FeeStatisticsResponse.builder()
                .billingMonth(targetMonth.toString())
                .classId(classId)
                .summary(summary)
                .monthlyStats(monthlyStats)
                .build();
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

    /**
     * FEE-10 할인 금액 결정.
     * discountPolicyId 가 없으면 기존처럼 관리자가 입력한 discountAmount 를 그대로 쓴다(하위호환).
     * discountPolicyId 가 있으면 DiscountPolicyService.preview() 로 활성/기간 검증 + 계산을 위임한다
     * (DCP-04 미리보기 API 와 계산 로직을 이원화하지 않기 위함 - 404/400 예외도 그대로 전파됨).
     */
    private long resolveDiscountAmount(Long discountPolicyId, Long manualDiscountAmount, long feeAmount) {
        if (discountPolicyId == null) {
            return manualDiscountAmount == null ? 0L : manualDiscountAmount;
        }
        return discountPolicyService.preview(discountPolicyId, feeAmount).getDiscountAmount();
    }

    /**
     * FEE-15/16(DCP-05): discountPolicyId 로 계산된 할인일 때만 fee_discounts 에 이력을 남긴다.
     * 관리자가 discountAmount 를 직접 입력한 경우(discountPolicyId 없음)는 정책 적용이 아니므로 이력을 남기지 않는다.
     * fee_discounts.applied_by 는 NOT NULL 이므로 currentUserId 가 반드시 있어야 한다(컨트롤러가 JWT 에서 채움).
     */
    private void recordDiscountHistory(Long feeId, Long discountPolicyId, long discountAmount, Long currentUserId) {
        if (discountPolicyId == null) {
            return;
        }
        FeeDiscountVo history = FeeDiscountVo.builder()
                .feeId(feeId)
                .discountPolicyId(discountPolicyId)
                .discountAmount(BigDecimal.valueOf(discountAmount))
                .appliedBy(currentUserId)
                .build();
        feeDiscountMapper.insert(history);
    }
}
