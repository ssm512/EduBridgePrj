package com.edu.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

// 설정 파일(application.yml)에서 app.jwt로 시작하는 값을 읽어오겠다
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        String issuer,
        String secret,
        long accessTokenMinutes,
        long refreshTokenDays
) {
}


