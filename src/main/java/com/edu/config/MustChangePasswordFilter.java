package com.edu.config;

import com.edu.domain.member.mapper.UserMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * [추가 2026-07-16] 코드점검 우선순위3-1 대응.
 *
 * 지금까지 users.must_change_password(관리자 초기화 후 강제 변경 플래그)는 로그인 응답에만
 * 실려서 프론트가 변경 페이지로 "유도"만 했고, 서버는 강제하지 않았다. 사용자가 리다이렉트를
 * 무시하면 임시 비밀번호로 계속 다른 API를 쓸 수 있었다.
 *
 * JWT에는 이 플래그를 claim으로 넣지 않는다 — 비밀번호 변경 성공 직후에도 같은 액세스 토큰이
 * 만료 전까지 그대로 쓰이는 구조라(AuthController#changePassword가 토큰을 재발급하지 않음),
 * claim에 굳혀 넣으면 "방금 변경했는데도 계속 막히는" 버그가 생긴다. 그래서 매 요청마다
 * DB의 최신 값을 가볍게 조회한다(selectMustChangePasswordByLoginId, 비밀번호 해시 등은 안 읽음).
 *
 * 페이지 라우팅(Thymeleaf)이 아니라 /api/** 요청만 막는다 — 화면 이동 자체는 막을 이유가 없고
 * (막힌 화면은 데이터 fetch가 실패하며 자연히 못 쓰게 된다), 명세서 밖 영역까지 건드리지 않기
 * 위해서다. 비밀번호 변경 자체와 최소한의 이탈 경로(로그아웃/토큰 재발급/CSRF)는 허용한다.
 */
public class MustChangePasswordFilter extends OncePerRequestFilter {

    /** must_change_password=TRUE 상태에서도 허용하는 API (이거 막으면 영원히 못 빠져나옴) */
    private static final Set<String> ALLOWED_PATHS = Set.of(
            "/api/auth/password",     // 본인 비밀번호 변경 - 여기로 빠져나간다
            "/api/auth/csrf",         // 변경 요청 전 CSRF 토큰 확보
            "/api/auth/me",           // 내 정보 확인 (마이페이지 등)
            "/api/auth/refresh",      // 액세스 토큰 재발급 - 막으면 강제로 로그아웃되는 셈
            "/api/auth/logout",
            "/api/auth/logout-web"
    );

    private final UserMapper userMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MustChangePasswordFilter(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();

        if (!path.startsWith("/api/") || ALLOWED_PATHS.contains(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            // 미인증 요청은 이 필터의 관심사가 아니다 (뒤이은 인가 처리에서 401 처리됨)
            filterChain.doFilter(request, response);
            return;
        }

        Boolean mustChange = userMapper.selectMustChangePasswordByLoginId(authentication.getName());
        if (Boolean.TRUE.equals(mustChange)) {
            writeForbidden(response, path);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void writeForbidden(HttpServletResponse response, String path) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        // GlobalExceptionHandler.error()와 동일한 응답 형태로 맞춘다 (프론트 에러 처리 공용)
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", HttpStatus.FORBIDDEN.value());
        body.put("error", HttpStatus.FORBIDDEN.getReasonPhrase());
        body.put("message", "비밀번호를 변경해야 계속 이용할 수 있습니다");
        body.put("path", path);

        objectMapper.writeValue(response.getWriter(), body);
    }
}
