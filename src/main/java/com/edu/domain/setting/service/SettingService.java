package com.edu.domain.setting.service;

import com.edu.domain.setting.dto.request.SettingUpdateRequest;
import com.edu.domain.setting.vo.SystemSetting;

import java.util.List;

/**
 * 시스템설정 서비스.
 * 관리 화면용 CRUD + 다른 도메인이 운영값을 읽는 소비용 헬퍼(getInt/getLong/getDouble)를 함께 제공.
 */
public interface SettingService {

    /** 전체 설정 목록 */
    List<SystemSetting> getAll();

    /** 값/설명 수정 (updatedBy = loginId의 user_id) */
    SystemSetting update(String key, SettingUpdateRequest request, String loginId);

    /** 원시 값 (없으면 null) */
    String getValue(String key);

    /** 학원 이름 (미설정/공백이면 'EduBridge') - 웹 타이틀·앱 브랜딩용 */
    String getAcademyName();

    /** 정수값 (없거나 파싱 실패 시 기본값) */
    int getInt(String key, int defaultValue);

    /** long값 (없거나 파싱 실패 시 기본값) */
    long getLong(String key, long defaultValue);

    /** 실수값 (없거나 파싱 실패 시 기본값) */
    double getDouble(String key, double defaultValue);

    /**
     * 해당 날짜가 휴일(수업 없는 날)인지 — 결석 자동화 스킵 판정.
     * 우선순위: HOLIDAYS_EXCLUDE(정상수업 예외) > HOLIDAYS(개별) > HOLIDAYS_RECURRING(MM-DD 매년).
     */
    boolean isHoliday(java.time.LocalDate date);
}
