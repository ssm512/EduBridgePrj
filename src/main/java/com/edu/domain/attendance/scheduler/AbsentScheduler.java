package com.edu.domain.attendance.scheduler;

import com.edu.domain.attendance.mapper.AttendanceMapper;
import com.edu.domain.attendance.service.AttendanceService;
import com.edu.domain.setting.service.SettingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

/**
 * 결석 자동화 스케줄러 (ATT-08 자동).
 *
 * 5분마다 폴링하여 "오늘 요일에 수업이 있고, 종료 후 ABSENT_DELAY_MINUTES 지난 반"을 찾아
 * 미출석자를 결석 처리한다. markAbsent는 "그날 기록 없는 학생만" 처리하는 멱등 구조라
 * 반복 실행/강사 수동처리와 충돌하지 않는다.
 *
 * 전제: 자정 넘는 수업 없음(LocalTime 비교). @EnableScheduling은 메인 클래스에 활성.
 */
@Component
public class AbsentScheduler {

    private static final Logger log = LoggerFactory.getLogger(AbsentScheduler.class);

    private final SettingService settingService;
    private final AttendanceMapper attendanceMapper;
    private final AttendanceService attendanceService;

    public AbsentScheduler(SettingService settingService,
                           AttendanceMapper attendanceMapper,
                           AttendanceService attendanceService) {
        this.settingService = settingService;
        this.attendanceMapper = attendanceMapper;
        this.attendanceService = attendanceService;
    }

    /** 5분마다 실행 */
    @Scheduled(cron = "0 */5 * * * *")
    public void run() {
        LocalDate today = LocalDate.now();

        // 공휴일/임시휴강이면 전체 스킵
        if (settingService.isHoliday(today)) {
            log.debug("결석 자동화 스킵 - 휴일 {}", today);
            return;
        }

        // 오늘 요일 코드 (MON, TUE, ...)
        String dayCode = today.getDayOfWeek()
                .getDisplayName(TextStyle.SHORT, Locale.ENGLISH).toUpperCase(Locale.ENGLISH);

        long delay = settingService.getLong("ABSENT_DELAY_MINUTES", 10);
        LocalTime cutoff = LocalTime.now().minusMinutes(delay);

        List<Long> classIds = attendanceMapper.findEndedClasses(dayCode, cutoff);
        if (classIds.isEmpty()) {
            return;
        }

        int done = 0;
        for (Long classId : classIds) {
            try {
                int marked = attendanceService.markAbsent(classId, today); // 멱등: 미기록자만
                if (marked > 0) {
                    done += marked;
                    log.info("결석 자동처리 classId={} {}명", classId, marked);
                }
            } catch (Exception e) {
                // 한 반 실패해도 나머지는 계속 진행
                log.warn("결석 자동처리 실패 classId={}", classId, e);
            }
        }
        if (done > 0) {
            log.info("결석 자동화 완료 {} - 총 {}명", today, done);
        }
    }
}
