package com.edu.domain.fee.scheduler;

import com.edu.domain.fee.dto.response.FeeNotificationTargetResponse;
import com.edu.domain.fee.service.FeeService;
import com.edu.domain.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * 회비 알림 배치 (FEE-08 납부 예정 / FEE-09 미납).
 *
 * 설계 메모:
 * - 스케줄러 자체에는 트랜잭션을 걸지 않는다. 학생별 알림 생성은
 *   notificationService.notifyParentsOfStudentOnce() 가 각각 독립 트랜잭션으로 처리하므로,
 *   한 학생 처리가 실패해도 나머지 학생 알림은 그대로 나간다 (배치 내성).
 * - 중복 방지: 알림 제목에 "종류(예정/미납) + 청구월" 을 박고, 같은 제목이 이미 있으면
 *   건너뛴다 (팀 결정: 청구월 기준 1회). 예정/미납은 제목이 달라 서로를 막지 않는다.
 * - 회비 등록 시 나가는 1회성 "납부 안내"(FeeServiceImpl.createFee) 와도 제목이 달라 충돌 없음.
 */
@Component
public class FeeNotificationScheduler {

    private static final Logger log = LoggerFactory.getLogger(FeeNotificationScheduler.class);

    /** 납부 예정 알림을 기한 며칠 전에 보낼지 (팀 결정: 3일 전) */
    private static final int UPCOMING_LEAD_DAYS = 3;

    private final FeeService feeService;
    private final NotificationService notificationService;

    public FeeNotificationScheduler(FeeService feeService, NotificationService notificationService) {
        this.feeService = feeService;
        this.notificationService = notificationService;
    }

    /**
     * FEE-08 납부 예정 알림 - 매일 오전 9시.
     * 오늘 기준 UPCOMING_LEAD_DAYS(3일) 뒤에 기한이 도래하는 미완납 회비가 대상.
     * (반환값은 스케줄러에서는 무시되고, 수동 실행 엔드포인트에서만 사용)
     */
    @Scheduled(cron = "0 0 9 * * *")
    public int runUpcomingFeeNotifications() {
        LocalDate targetDue = LocalDate.now().plusDays(UPCOMING_LEAD_DAYS);
        List<FeeNotificationTargetResponse> targets = feeService.getFeesDueOn(targetDue);

        int created = 0;
        for (FeeNotificationTargetResponse t : targets) {
            String title = "[납부예정] " + t.getBillingMonth() + " 회비";
            String message = t.getBillingMonth() + " 회비 " + t.getBillableAmount()
                    + "원의 납부 기한이 " + t.getDueDate() + "입니다. 기한 내 납부 부탁드립니다.";
            try {
                created += notificationService.notifyParentsOfStudentOnce(
                        t.getStudentId(), "FEE", title, message);
            } catch (Exception e) {
                // 한 건 실패가 배치 전체를 멈추지 않도록 로그만 남기고 계속
                log.warn("납부 예정 알림 생성 실패 feeId={}, studentId={}", t.getFeeId(), t.getStudentId(), e);
            }
        }
        log.info("FEE-08 납부 예정 알림 배치 완료 - 대상 {}건, 생성 {}건", targets.size(), created);
        return created;
    }

    /**
     * FEE-09 미납 알림 - 매일 오전 9시 5분.
     * 납부 기한이 지난 미완납 회비가 대상 (FEE-07 미납 판정과 동일 조건).
     */
    @Scheduled(cron = "0 5 9 * * *")
    public int runOverdueFeeNotifications() {
        List<FeeNotificationTargetResponse> targets = feeService.getOverdueUnpaidFees();

        int created = 0;
        for (FeeNotificationTargetResponse t : targets) {
            String title = "[미납] " + t.getBillingMonth() + " 회비";
            String message = t.getBillingMonth() + " 회비 " + t.getBillableAmount()
                    + "원이 미납 상태입니다. (납부 기한: " + t.getDueDate() + ") 빠른 납부 부탁드립니다.";
            try {
                created += notificationService.notifyParentsOfStudentOnce(
                        t.getStudentId(), "FEE", title, message);
            } catch (Exception e) {
                log.warn("미납 알림 생성 실패 feeId={}, studentId={}", t.getFeeId(), t.getStudentId(), e);
            }
        }
        log.info("FEE-09 미납 알림 배치 완료 - 대상 {}건, 생성 {}건", targets.size(), created);
        return created;
    }
}
