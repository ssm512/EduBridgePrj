package com.edu.domain.notification.service;

import com.edu.domain.notification.mapper.DeviceTokenMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** FCM 디바이스 토큰 등록/해제 */
@Service
@Transactional
public class DeviceTokenService {

    private final DeviceTokenMapper deviceTokenMapper;

    public DeviceTokenService(DeviceTokenMapper deviceTokenMapper) {
        this.deviceTokenMapper = deviceTokenMapper;
    }

    /** 앱 로그인/토큰갱신 시 등록 (upsert) */
    public void register(Long userId, String token, String platform) {
        if (token == null || token.isBlank()) return;
        deviceTokenMapper.upsert(userId, token, platform);
    }

    /** 앱 로그아웃 시 해제 */
    public void unregister(String token) {
        if (token == null || token.isBlank()) return;
        deviceTokenMapper.deleteByToken(token);
    }
}
