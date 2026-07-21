package com.edu.config;

import com.edu.domain.member.mapper.UserMapper;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, UserMapper userMapper) throws Exception {
        http
                // ===== CSRF 활성화 (Double Submit Cookie 방식) =====
                // 세션이 없는(STATELESS) JWT API에서도 CSRF 방어를 켠다.
                // 1. 서버가 XSRF-TOKEN 쿠키를 내려줌 (httpOnly=false → JS가 읽을 수 있음)
                // 2. 클라이언트는 POST/PUT/DELETE 요청 시
                //    쿠키 값을 X-XSRF-TOKEN 헤더에 복사해서 전송
                // 3. 서버는 쿠키와 헤더 값을 비교해서 검증
                // 공격 사이트는 다른 도메인의 쿠키를 읽을 수 없으므로 CSRF가 차단된다.
                .csrf(csrf -> csrf
                        // 세션 대신 쿠키에 CSRF 토큰 저장 (STATELESS와 호환)
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        // SPA/JS 클라이언트용 토큰 처리 핸들러 (BREACH 보호 + 쿠키 강제 발급)
                        .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler())
                )

                // 세션 X
                // STATEFULL : 다음페이지에서 이전 페이지의 정보를 알 수 있음
                // STATELESS : 다음페이지에서 이전 페이지의 정보를 알 수 없음
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 경로에 대한 권한 설정
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/", "/loginForm.html", "/signInForm.html",
                                "/css/**", "/js/**", "/img/**", "/favicon.ico",
                                "/edudata/**"
                        ).permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // CSRF 토큰 발급용 (GET이라 CSRF 검증 대상 아님)
                        .requestMatchers(HttpMethod.GET, "/api/auth/csrf").permitAll()

                        // [변경 2026-07-14] REST API URL을 /api 프리픽스로 통일 (팀 결정)
                        // - 기존 "명세서 URL 그대로(프리픽스 없음)" 결정을 폐기
                        // - 회원가입용으로 남아 있던 POST /api/users permitAll 규칙은
                        //   회원관리 API(/api/users, ADMIN 전용)와 충돌(보안 구멍)하므로 삭제.
                        //   실제 회원가입은 POST /api/auth/signup (아래 permitAll 유지)

                        // [추가] 회원관리 API (명세서 USER-01~03) - /api/users, /api/users/{userId}
                        // 컨트롤러의 @PreAuthorize("hasRole('ADMIN')")와 이중 방어
                        .requestMatchers("/api/users", "/api/users/**").hasRole("ADMIN")

                        // [추가] 강사관리 API (명세서 TEA-01~03) - ADMIN 전용
                        .requestMatchers("/api/teachers", "/api/teachers/**").hasRole("ADMIN")

                        // [추가] 학생관리 API (명세서 STU-01~04) - API마다 허용 롤이 다름
                        // 등록/수정: ADMIN / 목록: ADMIN,TEACHER / 상세: 4개 롤(본인/자녀 검증은 서비스에서)
                        .requestMatchers(HttpMethod.POST, "/api/students").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/students/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/students").hasAnyRole("ADMIN", "TEACHER")
                        .requestMatchers(HttpMethod.GET, "/api/students/*")
                            .hasAnyRole("ADMIN", "TEACHER", "STUDENT", "PARENT")

                        // [추가] 반관리 API (명세서 CLS-01~04)
                        // 등록/수정: ADMIN / 목록/상세: ADMIN,TEACHER
                        .requestMatchers(HttpMethod.POST, "/api/classes").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/classes/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/classes", "/api/classes/*")
                            .hasAnyRole("ADMIN", "TEACHER")

                        // [추가] 수강관리 API (명세서 ENR-01~02 + 목록)
                        // 등록/해제: ADMIN / 목록: ADMIN,TEACHER
                        .requestMatchers(HttpMethod.POST, "/api/enrollments").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/enrollments/*/end").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/enrollments").hasAnyRole("ADMIN", "TEACHER")

                        // [추가] 학부모관리 API (명세서 PAR-01~03) - ADMIN 전용
                        // PAR-03(학생-학부모 연결)은 POST /api/students/{id}/parents
                        .requestMatchers("/api/parents", "/api/parents/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/students/*/parents").hasRole("ADMIN")
                        // 연결 내역 수정 (명세서 외 추가 API)
                        .requestMatchers(HttpMethod.PUT, "/api/students/*/parents/*").hasRole("ADMIN")

                        // [추가] 공지사항 API (명세서 NOT-01~08) - API마다 허용 롤이 다름
                        // 등록/수정/삭제/첨부업로드: ADMIN,TEACHER (TEACHER 본인 공지 검증은 서비스에서)
                        // 목록/상세/다운로드/읽음처리: 4개 롤 (대상자별 가시성은 서비스+SQL에서)
                        .requestMatchers(HttpMethod.POST, "/api/notices").hasAnyRole("ADMIN", "TEACHER")
                        .requestMatchers(HttpMethod.PUT, "/api/notices/*").hasAnyRole("ADMIN", "TEACHER")
                        .requestMatchers(HttpMethod.DELETE, "/api/notices/*").hasAnyRole("ADMIN", "TEACHER")
                        .requestMatchers(HttpMethod.POST, "/api/notices/*/files").hasAnyRole("ADMIN", "TEACHER")
                        .requestMatchers(HttpMethod.POST, "/api/notices/*/read")
                            .hasAnyRole("ADMIN", "TEACHER", "STUDENT", "PARENT")
                        .requestMatchers(HttpMethod.GET, "/api/notices", "/api/notices/*")
                            .hasAnyRole("ADMIN", "TEACHER", "STUDENT", "PARENT")
                        .requestMatchers(HttpMethod.GET, "/api/notice-files/*/download")
                            .hasAnyRole("ADMIN", "TEACHER", "STUDENT", "PARENT")
                        // 첨부 삭제 (명세서 외 추가 API)
                        .requestMatchers(HttpMethod.DELETE, "/api/notice-files/*").hasAnyRole("ADMIN", "TEACHER")
                        .requestMatchers(HttpMethod.POST,
                                "/api/auth/signup",
                                "/api/auth/login",
                                "/api/auth/refresh",
                                "/api/auth/logout",
                                "/api/auth/logout-web",
                                "/api/auth/password-reset"
                        ).permitAll()

                        // ===== 서버 렌더링 페이지: 역할별 접근 제어 =====
                        // 로그인 후 진입점(디스패처). 인증만 되어 있으면 됨.
                        .requestMatchers("/home").authenticated()
                        // 각 역할 전용 화면 (JWT roles 클레임의 ROLE_XXX 권한으로 검사)
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/teacherPage/**").hasRole("TEACHER")
                        .requestMatchers("/studentPage/**").hasRole("STUDENT")
                        .requestMatchers("/parentPage/**").hasRole("PARENT")
                        // [추가] 역할별 기능 페이지 (예: /teacher/attendance, /student/attendance, /parent/attendance)
                        .requestMatchers("/teacher/**").hasRole("TEACHER")
                        .requestMatchers("/student/**").hasRole("STUDENT")
                        .requestMatchers("/parent/**").hasRole("PARENT")

                        // 나머지 API/페이지는 인증 필요
                        .anyRequest().authenticated()
                )

                // JWT를 헤더(모바일/API) 또는 ACCESS_TOKEN 쿠키(웹)에서 읽는다.
                .oauth2ResourceServer(oauth2 -> oauth2
                        .bearerTokenResolver(new CookieBearerTokenResolver())
                        .authenticationEntryPoint(new HtmlAwareAuthenticationEntryPoint())
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                )

                // [추가 2026-07-20] 관리자 초기화 계정(must_change_password=TRUE)은 비밀번호를
                // 바꾸기 전까지 /api/**를 못 쓰게 서버에서 강제한다 (코드점검 우선순위3-1 대응).
                // JWT 인증이 SecurityContext에 채워진 뒤에 돌아야 하므로 그 필터 바로 뒤에 붙인다.
                .addFilterAfter(new MustChangePasswordFilter(userMapper), BearerTokenAuthenticationFilter.class)

                // 인증 안 된 요청: 브라우저는 로그인("/")으로, API는 401
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new HtmlAwareAuthenticationEntryPoint())
                );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 로그인할 때 사용하는 인증 관리자입니다.
    // AuthService
    //→ AuthenticationManager
    //→ DaoAuthenticationProvider
    //→ CustomUserDetailsService
    //→ UserRepository
    //→ PasswordEncoder.matches()
    //→ 인증 성공 또는 실패
    // 로그인 성공 후에야 JwtService가 Access Token을 발급
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }


    // JWT Access Token을 생성할 때 사용합니다. (로그인 성공 시 JwtService에서 사용)
    // 회원 로그인 성공
    // → JwtEncoder가 JWT 생성
    // → Access Token 응답
    @Bean
    public JwtEncoder jwtEncoder(JwtProperties jwtProperties) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(secretKey(jwtProperties)));
    }


    // JWT Access Token을 검증할 때 사용합니다. 요청이 들어올 때 Spring Security가 이 Decoder를 사용

    //검증하는 내용은 다음입니다.
    // JWT 서명이 올바른가?
    // 만료 시간이 지나지 않았는가?
    // 토큰 구조가 정상인가?

    // 현재 예제는 HS256 방식입니다.
    //  HS256 = 하나의 secret key로 서명도 하고 검증도 하는 방식
    //  즉, 서버가 가지고 있는 secret 값이 매우 중요합니다.
    @Bean
    public JwtDecoder jwtDecoder(JwtProperties jwtProperties) {
        return NimbusJwtDecoder
                .withSecretKey(secretKey(jwtProperties))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }


    // JWT 안의 권한 정보를 Spring Security 권한으로 바꿔주는 설정
    //  JWT 안의 roles claim을 권한 목록으로 사용하라
    // 기본적으로 Spring Security는 권한 ROLE_ JWT 권한 앞에 SCOPE_
    // JWT 권한 앞에 SCOPE_이므로 .setAuthorityPrefix("");
    //  JWT roles: ["ROLE_ADMIN"]  → Spring Security 권한: ROLE_ADMIN
    //  만약 prefix를 비우지 않으면 의도와 다른 권한명이 될 수 있습니다.
    // JWT SCOPE_ADMIN -> spring security의 ROLE_ADMIN으로 변환해줌
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthorityPrefix(""); // JWT 권한 앞에 있는 scope_제거
        authoritiesConverter.setAuthoritiesClaimName("roles");

        JwtAuthenticationConverter authenticationConverter = new JwtAuthenticationConverter();
        authenticationConverter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return authenticationConverter;
    }


    // application.yml에 있는 secret 문자열을 바이트 배열로 바꾼 뒤, HMAC SHA-256용 SecretKey로 만듭니다.
    // HMAC SHA-256 용으로 SecreyKey로 만드다.
    // 이 SecretKey는 두 곳에서 사용됩니다.
    // JwtEncoder → JWT 생성
    // JwtDecoder → JWT 검증
    private SecretKey secretKey(JwtProperties jwtProperties) {
        byte[] secretBytes = jwtProperties.secret().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return new SecretKeySpec(secretBytes, "HmacSHA256");
    }


}