package com.edu.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.util.StringUtils;

import java.util.function.Supplier;

/**
 * SPA(JavaScript fetch/axios) 클라이언트용 CSRF 토큰 핸들러.
 * Spring Security 공식 문서의 Single Page Application 패턴.
 *
 * 동작 방식
 * - 응답 렌더링 시: XorCsrfTokenRequestAttributeHandler 사용 (BREACH 공격 보호)
 * - csrfToken.get() 호출로 매 요청마다 토큰을 로드 → XSRF-TOKEN 쿠키가 실제로 내려가게 함
 *   (Spring Security는 기본적으로 토큰을 지연(deferred) 로드해서
 *    아무도 토큰을 읽지 않으면 쿠키가 발급되지 않는 문제가 있음)
 * - 요청 검증 시:
 *   헤더(X-XSRF-TOKEN)로 오면 → 쿠키 원문 그대로이므로 평문 비교
 *   파라미터(_csrf)로 오면   → XOR 인코딩된 값이므로 Xor 핸들러로 비교
 */
final class SpaCsrfTokenRequestHandler implements CsrfTokenRequestHandler {

    private final CsrfTokenRequestHandler plain = new CsrfTokenRequestAttributeHandler();
    private final CsrfTokenRequestHandler xor = new XorCsrfTokenRequestAttributeHandler();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       Supplier<CsrfToken> csrfToken) {
        // BREACH 보호를 적용해서 request attribute에 토큰 저장
        this.xor.handle(request, response, csrfToken);
        // 토큰을 강제로 로드해서 XSRF-TOKEN 쿠키가 응답에 포함되도록 함
        csrfToken.get();
    }

    @Override
    public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
        String headerValue = request.getHeader(csrfToken.getHeaderName());
        // 헤더로 온 값(쿠키 원문)은 평문 비교, 폼 파라미터는 XOR 디코딩 비교
        return (StringUtils.hasText(headerValue) ? this.plain : this.xor)
                .resolveCsrfTokenValue(request, csrfToken);
    }
}
