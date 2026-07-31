package com.edu.domain.notification.controller;

import com.edu.domain.notification.dto.request.DeviceTokenRequest;
import com.edu.domain.notification.service.DeviceTokenService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * FCM 디바이스 토큰 등록/해제 (앱 전용).
 * 로그인 후 앱이 토큰을 등록하고, 로그아웃 시 해제한다.
 */
@RestController
@RequestMapping("/api/device-tokens")
public class DeviceTokenController {

    private final DeviceTokenService deviceTokenService;

    public DeviceTokenController(DeviceTokenService deviceTokenService) {
        this.deviceTokenService = deviceTokenService;
    }

    /** POST /api/device-tokens - 토큰 등록/갱신 */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public Map<String, Boolean> register(@RequestBody DeviceTokenRequest request,
                                         JwtAuthenticationToken authentication) {
        deviceTokenService.register(currentUserId(authentication), request.token(), request.platform());
        return Map.of("ok", true);
    }

    /** DELETE /api/device-tokens?token=... - 토큰 해제 (로그아웃) */
    @DeleteMapping
    @PreAuthorize("isAuthenticated()")
    public Map<String, Boolean> unregister(@RequestParam String token) {
        deviceTokenService.unregister(token);
        return Map.of("ok", true);
    }

    private Long currentUserId(JwtAuthenticationToken authentication) {
        return ((Number) authentication.getToken().getClaim("userId")).longValue();
    }
}
