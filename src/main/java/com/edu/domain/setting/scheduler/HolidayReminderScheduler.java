package com.edu.domain.setting.scheduler;

import com.edu.domain.notification.service.NotificationService;
import com.edu.domain.setting.mapper.SettingMapper;
import com.edu.domain.setting.service.SettingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/**
 * 공휴일 등록 리마인더.
 *
 * 음력 공휴일(설날·추석)·임시휴강은 매년 날짜가 달라 관리자가 직접 등록해야 한다.
 * 등록을 잊으면 그날 결석 자동처리가 잘못 실행돼 전원 오결석 + 학부모 오알림이 발생하므로,
 * 매월 1일에 "올해 개별공휴일(HOLIDAYS)이 하나도 없으면" 관리자 전원에게 알림을 보낸다.
 *
 * 관리자가 올해 공휴일을 하나라도 등록하면 조건이 거짓이 되어 자동으로 멈춘다(별도 마커 불필요).
 * 알림 발송은 윤동재님 NotificationService.createNotification 를 호출(생성 API의 의도된 사용).
 */
@Component
public class HolidayReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(HolidayReminderScheduler.class);

    private final SettingService settingService;
    private final SettingMapper settingMapper;
    private final NotificationService notificationService;

    public HolidayReminderScheduler(SettingService settingService,
                                    SettingMapper settingMapper,
                                    NotificationService notificationService) {
        this.settingService = settingService;
        this.settingMapper = settingMapper;
        this.notificationService = notificationService;
    }

    /** 매월 1일 09:00 */
    @Scheduled(cron = "0 0 9 1 * *")
    public void remind() {
        int year = LocalDate.now().getYear();

        // 올해(YYYY-) 개별공휴일이 하나라도 등록돼 있으면 스킵
        String holidays = settingService.getValue("HOLIDAYS");
        boolean hasThisYear = holidays != null && Arrays.stream(holidays.split(","))
                .map(String::trim)
                .anyMatch(s -> s.startsWith(year + "-"));
        if (hasThisYear) {
            return;
        }

        List<Long> adminIds = settingMapper.findAdminUserIds();
        if (adminIds.isEmpty()) {
            return;
        }

        String title = "공휴일 등록 필요";
        String message = year + "년 공휴일이 아직 등록되지 않았습니다. 설날·추석 등 음력 공휴일과 임시휴강을 "
                + "[시스템설정 > 휴일 관리]에서 등록해 주세요. 미등록 시 해당일에 결석이 잘못 자동처리될 수 있습니다.";

        int sent = 0;
        for (Long userId : adminIds) {
            try {
                notificationService.createNotification(userId, "NOTICE", title, message);
                sent++;
            } catch (Exception e) {
                log.warn("공휴일 리마인더 발송 실패 userId={}", userId, e);
            }
        }
        log.info("공휴일 등록 리마인더 발송 - {}년, 관리자 {}명", year, sent);
    }
}
