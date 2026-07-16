package com.edu.domain.setting.service.impl;

import com.edu.domain.setting.dto.request.SettingUpdateRequest;
import com.edu.domain.setting.mapper.SettingMapper;
import com.edu.domain.setting.service.SettingService;
import com.edu.domain.setting.vo.SystemSetting;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class SettingServiceImpl implements SettingService {

    public static final String ACTION_SETTING_CHANGED = "SETTING_CHANGED";

    private final SettingMapper settingMapper;

    public SettingServiceImpl(SettingMapper settingMapper) {
        this.settingMapper = settingMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SystemSetting> getAll() {
        return settingMapper.findAll();
    }

    @Override
    @Transactional
    public SystemSetting update(String key, SettingUpdateRequest request, String loginId) {
        if (request.settingValue() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "설정값은 필수입니다.");
        }
        Long updatedBy = loginId == null ? null : settingMapper.findUserIdByLoginId(loginId);
        int updated = settingMapper.updateValue(key, request.settingValue(), request.description(), updatedBy);
        String desc = key + " 설정을 [" + request.settingValue() + "] 으로 변경";
        settingMapper.insertActivityLog(updatedBy, ACTION_SETTING_CHANGED, "system_settings", desc);
        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "설정을 찾을 수 없습니다: " + key);
        }
        return settingMapper.findByKey(key);
    }

    @Override
    @Transactional(readOnly = true)
    public String getValue(String key) {
        return settingMapper.getValue(key);
    }

    @Override
    public int getInt(String key, int defaultValue) {
        String v = getValue(key);
        if (v == null) return defaultValue;
        try { return Integer.parseInt(v.trim()); } catch (NumberFormatException e) { return defaultValue; }
    }

    @Override
    public long getLong(String key, long defaultValue) {
        String v = getValue(key);
        if (v == null) return defaultValue;
        try { return Long.parseLong(v.trim()); } catch (NumberFormatException e) { return defaultValue; }
    }

    @Override
    public double getDouble(String key, double defaultValue) {
        String v = getValue(key);
        if (v == null) return defaultValue;
        try { return Double.parseDouble(v.trim()); } catch (NumberFormatException e) { return defaultValue; }
    }

    private static final DateTimeFormatter MMDD = DateTimeFormatter.ofPattern("MM-dd");

    @Override
    @Transactional(readOnly = true)
    public boolean isHoliday(LocalDate date) {
        if (parseDates("HOLIDAYS_EXCLUDE").contains(date)) return false;   // 정상수업 예외가 최우선
        if (parseDates("HOLIDAYS").contains(date)) return true;           // 개별 공휴일/임시휴강
        return parseTokens("HOLIDAYS_RECURRING").contains(date.format(MMDD)); // 양력 고정(매년)
    }

    /** CSV(YYYY-MM-DD) → LocalDate 집합 (형식 오류 항목은 무시) */
    private Set<LocalDate> parseDates(String key) {
        Set<LocalDate> out = new HashSet<>();
        for (String s : parseTokens(key)) {
            try { out.add(LocalDate.parse(s)); } catch (Exception ignore) { /* 잘못된 형식 무시 */ }
        }
        return out;
    }

    /** CSV 문자열 → 공백/빈값 제거한 토큰 집합 */
    private Set<String> parseTokens(String key) {
        String v = getValue(key);
        Set<String> out = new HashSet<>();
        if (v == null || v.isBlank()) return out;
        for (String s : v.split(",")) {
            String t = s.trim();
            if (!t.isEmpty()) out.add(t);
        }
        return out;
    }
}
