package com.edu.domain.attendance.scheduler;

import com.edu.domain.attendance.mapper.AttendanceMapper;
import com.edu.domain.attendance.service.AttendanceService;
import com.edu.domain.setting.service.SettingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
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
 * 1) 평시: 5분마다 폴링하여 "오늘 요일에 수업이 있고, 종료 후 ABSENT_DELAY_MINUTES 지난 반"의
 *    미출석자를 결석 처리한다.
 * 2) 재가동 백필: 서버가 며칠 꺼져 있다가 켜지면, 지난 ABSENT_BACKFILL_DAYS(기본 7)일 중
 *    처리 안 된 날짜를 되짚어 결석 처리한다. → 장기 다운 후에도 "계속 켜둔 것"과 사실상 동일.
 *
 * markAbsent는 "그날 기록 없는 학생만" 처리하는 멱등 구조라, 반복 실행/백필/강사 수동처리와
 * 충돌하지 않는다(이미 처리된 날짜는 후보 0명 → 알림 중복도 없음).
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

    /** 평시: 5분마다 오늘분 처리 (종료 후 지연시간 지난 반) */
    @Scheduled(cron = "0 */5 * * * *")
    public void run() {
        long delay = settingService.getLong("ABSENT_DELAY_MINUTES", 10);
        processDay(LocalDate.now(), LocalTime.now().minusMinutes(delay));
    }

    /**
     * 재가동 시 지난 N일 백필.
     * 서버가 꺼져 있던 날짜의 결석을 채운다. 이미 처리된 날은 멱등이라 변화 없음.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void backfillOnStartup() {
        int days = (int) settingService.getLong("ABSENT_BACKFILL_DAYS", 7);
        if (days <= 0) {
            return;
        }
        // 초기 배포 방어: 출석기록이 전혀 없으면(=이제 막 시작) 백필하지 않는다.
        LocalDate last = attendanceMapper.findLatestAttendanceDate();
        if (last == null) {
            log.info("결석 백필 스킵 - 출석기록 없음(초기 상태)");
            return;
        }
        LocalDate today = LocalDate.now();
        // 마지막 기록일 포함(그날 낮에 꺼졌으면 늦은 수업 누락분 회수 — 멱등이라 안전),
        // 단 최대 N일 이내로 제한, 어제까지. (오늘은 5분 폴링이 담당)
        LocalDate start = last;
        LocalDate earliest = today.minusDays(days);
        if (start.isBefore(earliest)) {
            start = earliest;   // 너무 오래 꺼져 있었으면 N일치만
        }
        int scanned = 0;
        for (LocalDate date = start; date.isBefore(today); date = date.plusDays(1)) {
            // 지난 날짜는 하루가 이미 지났으므로 종료시각 컷오프를 하루 끝으로 둔다.
            processDay(date, LocalTime.MAX);
            scanned++;
        }
        if (scanned > 0) {
            log.info("결석 백필 점검 완료 - {} ~ 어제 ({}일)", start, scanned);
        }
    }

    /**
     * 특정 날짜의 결석 처리.
     * @param cutoff 이 시각 이전에 종료된 반만 대상 (오늘=현재-지연, 과거일=하루끝)
     */
    private void processDay(LocalDate date, LocalTime cutoff) {
        // 공휴일/임시휴강이면 스킵
        if (settingService.isHoliday(date)) {
            return;
        }
        String dayCode = date.getDayOfWeek()
                .getDisplayName(TextStyle.SHORT, Locale.ENGLISH).toUpperCase(Locale.ENGLISH);

        List<Long> classIds = attendanceMapper.findEndedClasses(dayCode, cutoff);
        if (classIds.isEmpty()) {
            return;
        }

        int done = 0;
        for (Long classId : classIds) {
            try {
                int marked = attendanceService.markAbsent(classId, date); // 멱등: 미기록자만
                if (marked > 0) {
                    done += marked;
                    log.info("결석 자동처리 {} classId={} {}명", date, classId, marked);
                }
            } catch (Exception e) {
                // 한 반 실패해도 나머지는 계속 진행
                log.warn("결석 자동처리 실패 {} classId={}", date, classId, e);
            }
        }
        if (done > 0) {
            log.info("결석 처리 완료 {} - 총 {}명", date, done);
        }
    }
}
