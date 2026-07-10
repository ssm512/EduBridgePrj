package com.edu.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

import java.io.IOException;

/**
 * 인증되지 않은 요청이 보호된 리소스에 접근했을 때의 처리 분기.
 *
 * - 브라우저 페이지 요청(Accept: text/html) → 로그인 페이지("/")로 리다이렉트
 * - API 요청(그 외)                          → 401 Unauthorized (JSON 클라이언트용)
 *
 * 이렇게 해야 웹 사용자는 토큰이 없거나 만료됐을 때 로그인 화면으로 자연스럽게
 * 돌아가고, 모바일/API 클라이언트는 기존처럼 401을 받는다.
 */
public class HtmlAwareAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final AuthenticationEntryPoint browserEntryPoint = new LoginUrlAuthenticationEntryPoint("/");
    private final AuthenticationEntryPoint apiEntryPoint = new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED);

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {
        String accept = request.getHeader("Accept");
        boolean isHtml = accept != null && accept.contains("text/html");
        if (isHtml) {
            browserEntryPoint.commence(request, response, authException);
        } else {
            apiEntryPoint.commence(request, response, authException);
        }
    }
}
