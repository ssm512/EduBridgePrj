package com.edu.common.util;

import com.edu.config.CookieBearerTokenResolver;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

/**
 * 웹 브라우저용 Access Token 쿠키 발급/제거 유틸.
 *
 * - httpOnly=true  : JS가 못 읽음 → XSS로 토큰 탈취 방지
 * - sameSite=Lax   : 최상위 페이지 이동 시 쿠키 전송 허용(로그인 후 리다이렉트에 필요),
 *                    크로스사이트 POST는 차단 → CSRF 완화
 * - secure=false   : 로컬(http) 개발용. 운영(https) 배포 시 true 로 변경할 것.
 * - path=/         : 전체 경로에서 전송
 */
public final class CookieUtil {

    private CookieUtil() {
    }

    /** 로그인 성공 시 Access Token을 쿠키로 내려준다. maxAge는 토큰 만료(초)와 맞춘다. */
    public static void addAccessTokenCookie(HttpServletResponse response, String accessToken, long maxAgeSeconds) {
        ResponseCookie cookie = ResponseCookie.from(CookieBearerTokenResolver.ACCESS_TOKEN_COOKIE, accessToken)
                .httpOnly(true)
                .secure(false)        // TODO: 운영(HTTPS)에서는 true
                .path("/")
                .sameSite("Lax")
                .maxAge(maxAgeSeconds)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    /** 로그아웃 시 Access Token 쿠키를 제거한다. */
    public static void clearAccessTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(CookieBearerTokenResolver.ACCESS_TOKEN_COOKIE, "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .sameSite("Lax")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
