package com.edu.domain.fee.controller;

import com.edu.common.dto.PageResponse;
import com.edu.domain.fee.dto.request.FeeCreateRequest;
import com.edu.domain.fee.dto.request.FeePaymentRequest;
import com.edu.domain.fee.dto.request.FeeSearchRequest;
import com.edu.domain.fee.dto.request.FeeUpdateRequest;
import com.edu.domain.fee.dto.response.FeeCreateResponse;
import com.edu.domain.fee.dto.response.FeeListResponse;
import com.edu.domain.fee.dto.response.FeeNotificationRunResponse;
import com.edu.domain.fee.dto.response.FeePaymentHistoryResponse;
import com.edu.domain.fee.dto.response.FeePaymentResponse;
import com.edu.domain.fee.dto.response.FeeStatisticsResponse;
import com.edu.domain.fee.dto.response.FeeUpdateResponse;
import com.edu.domain.fee.scheduler.FeeNotificationScheduler;
import com.edu.domain.fee.service.FeeService;
import jakarta.validation.Valid;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 회비 REST API (FEE-01 ~ 06)
 * base path 는 API 명세서 공통규격(/api/v1) 기준
 */
@RestController
@RequestMapping("/api/v1/fees")
public class FeeApiController {

    private final FeeService feeService;
    private final FeeNotificationScheduler feeNotificationScheduler;

    public FeeApiController(FeeService feeService,
                           FeeNotificationScheduler feeNotificationScheduler) {
        this.feeService = feeService;
        this.feeNotificationScheduler = feeNotificationScheduler;
    }

    /**
     * GET /api/v1/fees - 회비 목록/납부 이력 조회 (FEE-06) + 미납 조회 (FEE-07)
     * ?studentId=&billingMonth=YYYY-MM&statusCode=&overdueOnly=true&page=1&size=10
     * overdueOnly=true 면 납부 기한이 지난 미완납 건만 조회 (FEE-07)
     * TODO: PARENT 는 본인 자녀 회비만 조회되도록 데이터 범위 제한 필요 (팀 논의)
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'PARENT')")
    public PageResponse<FeeListResponse> getFeeList(@ModelAttribute FeeSearchRequest search) {
        return feeService.getFeeList(search);
    }

    /** POST /api/v1/fees - 회비 등록 (FEE-01) */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public FeeCreateResponse createFee(@Valid @RequestBody FeeCreateRequest request) {
        return feeService.createFee(request);
    }

    /**
     * GET /api/v1/fees/statistics - 회비 통계 (FEE-06)
     * ?billingMonth=YYYY-MM(기본: 이번 달)&classId=
     * 주의: /{feeId} 같은 경로 변수 매핑이 생기면 /statistics 가 먼저 매칭되는지 확인 필요
     *      (Spring 은 정확히 일치하는 패턴을 우선하므로 현재는 문제 없음)
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public FeeStatisticsResponse getStatistics(@RequestParam(required = false) String billingMonth,
                                               @RequestParam(required = false) Long classId) {
        return feeService.getStatistics(billingMonth, classId);
    }

    /** PUT /api/v1/fees/{feeId} - 회비 수정 (FEE-03) */
    @PutMapping("/{feeId}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public FeeUpdateResponse updateFee(@PathVariable Long feeId,
                                       @Valid @RequestBody FeeUpdateRequest request) {
        return feeService.updateFee(feeId, request);
    }

    /** POST /api/v1/fees/{feeId}/payments - 납부 처리 (FEE-04) */
    @PostMapping("/{feeId}/payments")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public FeePaymentResponse payFee(@PathVariable Long feeId,
                                     @Valid @RequestBody FeePaymentRequest request) {
        return feeService.payFee(feeId, request);
    }

    /**
     * GET /api/v1/fees/{feeId}/payments - 회비 1건의 납부 이력 조회 (FEE-06)
     * 취소분 포함, 최신순. POST 와 경로는 같지만 HTTP 메서드가 달라 매핑 충돌 없음.
     */
    @GetMapping("/{feeId}/payments")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'PARENT')")
    public List<FeePaymentHistoryResponse> getPaymentHistory(@PathVariable Long feeId) {
        return feeService.getPaymentHistory(feeId);
    }

    /**
     * POST /api/v1/fees/notifications/run - 회비 예정/미납 알림 배치 수동 실행 (FEE-08/09)
     * 정상 운영에서는 매일 스케줄러가 자동 실행하지만, 즉시 실행/테스트가 필요할 때 사용.
     * 명세서 외 추가. 경로가 2세그먼트(notifications/run)라 /{feeId} 매핑과 충돌 없음.
     */
    @PostMapping("/notifications/run")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public FeeNotificationRunResponse runFeeNotifications() {
        int upcoming = feeNotificationScheduler.runUpcomingFeeNotifications();
        int overdue = feeNotificationScheduler.runOverdueFeeNotifications();
        return new FeeNotificationRunResponse(upcoming, overdue);
    }

    /**
     * DELETE /api/v1/fees/{feeId} - 회비 삭제 (명세서 외 추가, 2026-07-15)
     * 잘못 등록된 청구 정리용. 납부 이력(취소분 포함)이 있으면 409.
     * 근거: 잘못 등록된 UNPAID 건이 미납 통계를 오염시키는 문제 해결 - 팀 공유 안건
     */
    @DeleteMapping("/{feeId}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFee(@PathVariable Long feeId) {
        feeService.deleteFee(feeId);
    }
}
