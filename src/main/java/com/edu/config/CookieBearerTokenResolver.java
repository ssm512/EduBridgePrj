package com.edu.config;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;

/**
 * JWT Access Token을 두 곳 중 하나에서 읽는다.
 *
 * 1) Authorization: Bearer <token> 헤더   → 모바일 앱 / 외부 API 클라이언트
 * 2) ACCESS_TOKEN 쿠키 (httpOnly)          → 웹 브라우저(서버 렌더링 Thymeleaf 페이지)
 *
 * 브라우저는 링크 클릭·새로고침 같은 "페이지 이동" 시 헤더는 못 붙이지만
 * 쿠키는 자동으로 실어 보낸다. 그래서 같은 JWT를 쿠키로도 받게 해서
 * 서버측 hasRole() / Thymeleaf 권한 렌더링 / 역할별 리다이렉트가 동작하게 한다.
 *
 * 헤더가 우선이므로 모바일/외부 API는 기존과 100% 동일하게 동작한다.
 */
public class CookieBearerTokenResolver implements BearerTokenResolver {

    public static final String ACCESS_TOKEN_COOKIE = "ACCESS_TOKEN";
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    public String resolve(HttpServletRequest request) {
        // 1순위: Authorization 헤더 (모바일/API)
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith(BEARER_PREFIX)) {
            String token = authorization.substring(BEARER_PREFIX.length()).trim();
            if (!token.isEmpty()) {
                return token;
            }
        }

        // 2순위: ACCESS_TOKEN 쿠키 (웹 브라우저)
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (ACCESS_TOKEN_COOKIE.equals(cookie.getName())) {
                    String value = cookie.getValue();
                    if (value != null && !value.isBlank()) {
                        return value;
                    }
                }
            }
        }
        return null;
    }
}
