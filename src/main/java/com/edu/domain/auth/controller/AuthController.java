package com.edu.domain.auth.controller;

import com.edu.domain.auth.dto.AuthResponse;
import com.edu.domain.auth.dto.LoginRequest;
import com.edu.domain.auth.dto.MessageResponse;
import com.edu.domain.auth.dto.MyPageUpdateRequest;
import com.edu.domain.auth.dto.PasswordChangeRequest;
import com.edu.domain.auth.dto.PasswordResetRequest;
import com.edu.domain.auth.dto.SignupRequest;
import com.edu.domain.auth.dto.TokenRefreshRequest;
import com.edu.domain.auth.dto.UserResponse;
import com.edu.domain.auth.service.AuthService;
import com.edu.common.util.CookieUtil;
import com.edu.domain.member.mapper.UserMapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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

    /**
     * POST /api/auth/login - 로그인
     * - 모바일/API: 응답 body의 accessToken(Bearer 헤더용)을 사용
     * - 웹 브라우저: 같은 accessToken을 httpOnly 쿠키(ACCESS_TOKEN)로도 내려줌
     *   → 이후 페이지 이동/새로고침 시 쿠키가 자동으로 실려 서버측 권한 검사가 동작
     */
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        AuthResponse auth = authService.login(request);
        CookieUtil.addAccessTokenCookie(response, auth.accessToken(), auth.expiresIn());
        return auth;
    }

    /** POST /api/auth/refresh - Access Token 재발급 */
    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody TokenRefreshRequest request) {
        return authService.refresh(request);
    }

    /** POST /api/auth/logout - Refresh Token 폐기 (모바일/API용) */
    @PostMapping("/logout")
    public MessageResponse logout(@Valid @RequestBody TokenRefreshRequest request) {
        authService.logout(request);
        return new MessageResponse("로그아웃되었습니다");
    }

    /**
     * POST /api/auth/logout-web - 웹 로그아웃
     * 브라우저는 refreshToken을 갖고 있지 않으므로(쿠키 기반) ACCESS_TOKEN 쿠키만 제거한다.
     * 이후 프론트에서 "/"로 이동.
     */
    @PostMapping("/logout-web")
    public MessageResponse logoutWeb(HttpServletResponse response) {
        CookieUtil.clearAccessTokenCookie(response);
        return new MessageResponse("로그아웃되었습니다");
    }

    /**
     * POST /api/auth/password-reset - 셀프 비밀번호 재설정 (비로그인)
     * 아이디+이메일 일치 시 임시 비밀번호를 이메일로 발송한다. (개발 단계: 콘솔 출력)
     * 계정 존재 여부를 노출하지 않기 위해 항상 동일한 응답을 반환한다.
     */
    @PostMapping("/password-reset")
    public MessageResponse passwordReset(@Valid @RequestBody PasswordResetRequest request) {
        authService.requestPasswordReset(request);
        return new MessageResponse("입력하신 정보가 일치하면 등록된 이메일로 임시 비밀번호가 발송됩니다");
    }

    /**
     * PUT /api/auth/password - 본인 비밀번호 변경 (인증 필요)
     * 현재 비밀번호 확인 후 교체. 성공 시 기존 Refresh Token 전부 폐기되므로
     * 다른 기기에서는 재로그인이 필요하다.
     */
    @PutMapping("/password")
    public MessageResponse changePassword(@Valid @RequestBody PasswordChangeRequest request,
                                          Authentication authentication) {
        authService.changePassword(authentication.getName(), request);
        return new MessageResponse("비밀번호가 변경되었습니다");
    }

    /** GET /api/auth/me - 내 정보 (Bearer Access Token 필요) */
    @GetMapping("/me")
    public UserResponse me(Authentication authentication) {
        return UserResponse.from(userMapper.selectByLoginId(authentication.getName()));
    }

    /**
     * PUT /api/auth/me - 마이페이지 본인 정보 수정 (인증 필요)
     * 이름/이메일/연락처만 수정 가능. 아이디/권한/상태는 이 API로 바꿀 수 없다.
     */
    @PutMapping("/me")
    public UserResponse updateMe(@Valid @RequestBody MyPageUpdateRequest request,
                                 Authentication authentication) {
        return authService.updateMyInfo(authentication.getName(), request);
    }
}
