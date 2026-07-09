package com.edu.domain.auth.dto;

/**
 * 로그인 / 토큰 재발급 응답
 * accessToken  : JWT (Authorization: Bearer 헤더로 사용)
 * refreshToken : 랜덤 문자열 (재발급용, DB에는 해시만 저장)
 */
public record AuthResponse(
        String tokenType,
        String accessToken,
        String refreshToken,
        long expiresIn,
        UserResponse user
) {
    public static AuthResponse bearer(String accessToken, String refreshToken,
                                      long expiresIn, UserResponse user) {
        return new AuthResponse("Bearer", accessToken, refreshToken, expiresIn, user);
    }
}
