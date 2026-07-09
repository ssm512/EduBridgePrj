package com.edu.domain.auth.controller;

import com.edu.domain.auth.dto.AuthResponse;
import com.edu.domain.auth.dto.LoginRequest;
import com.edu.domain.auth.dto.MessageResponse;
import com.edu.domain.auth.dto.SignupRequest;
import com.edu.domain.auth.dto.TokenRefreshRequest;
import com.edu.domain.auth.dto.UserResponse;
import com.edu.domain.auth.service.AuthService;
import com.edu.domain.member.mapper.UserMapper;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserMapper userMapper;

    public AuthController(AuthService authService, UserMapper userMapper) {
        this.authService = authService;
        this.userMapper = userMapper;
    }

    /**
     * GET /api/auth/csrf
     * CSRF 토큰 발급용 엔드포인트.
     * 이 요청을 호출하면 XSRF-TOKEN 쿠키가 내려간다.
     * 이후 POST 요청 시 쿠키 값을 X-XSRF-TOKEN 헤더에 담아 보내야 한다.
     */
    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken csrfToken) {
        return Map.of(
                "headerName", csrfToken.getHeaderName(),
                "token", csrfToken.getToken()
        );
    }

    /** POST /api/auth/signup - 회원가입 */
    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse signup(@Valid @RequestBody SignupRequest request) {
        return authService.signup(request);
    }

    /** POST /api/auth/login - 로그인 */
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    /** POST /api/auth/refresh - Access Token 재발급 */
    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody TokenRefreshRequest request) {
        return authService.refresh(request);
    }

    /** POST /api/auth/logout - Refresh Token 폐기 */
    @PostMapping("/logout")
    public MessageResponse logout(@Valid @RequestBody TokenRefreshRequest request) {
        authService.logout(request);
        return new MessageResponse("로그아웃되었습니다");
    }

    /** GET /api/auth/me - 내 정보 (Bearer Access Token 필요) */
    @GetMapping("/me")
    public UserResponse me(Authentication authentication) {
        return UserResponse.from(userMapper.selectByLoginId(authentication.getName()));
    }
}
