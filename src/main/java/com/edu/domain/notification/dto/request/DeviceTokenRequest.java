package com.edu.domain.notification.dto.request;

/**
 * FCM 디바이스 토큰 등록 요청.
 * @param token    FCM 등록 토큰
 * @param platform ANDROID / IOS (선택)
 */
public record DeviceTokenRequest(String token, String platform) {
}
