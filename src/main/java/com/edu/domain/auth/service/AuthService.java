package com.edu.domain.auth.service;

import com.edu.common.exception.ApiException;
import com.edu.domain.auth.dto.AuthResponse;
import com.edu.domain.auth.dto.LoginRequest;
import com.edu.domain.auth.dto.SignupRequest;
import com.edu.domain.auth.dto.TokenRefreshRequest;
import com.edu.domain.auth.dto.UserResponse;
import com.edu.domain.member.dto.UserDto;
import com.edu.domain.member.mapper.UserMapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 인증 흐름의 중심 서비스.
 * - 아이디/비밀번호 검증 → AuthenticationManager
 * - Access Token 생성   → JwtService
 * - Refresh Token 관리  → RefreshTokenService
 * - 회원가입            → UserMapper + PasswordEncoder
 */
@Service
@Transactional
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserMapper userMapper;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;

    public AuthService(AuthenticationManager authenticationManager,
                       UserMapper userMapper,
                       JwtService jwtService,
                       RefreshTokenService refreshTokenService,
                       PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.userMapper = userMapper;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.passwordEncoder = passwordEncoder;
    }

    /** 회원가입 */
    public UserResponse signup(SignupRequest request) {
        if (userMapper.selectByLoginId(request.loginId()) != null) {
            throw new ApiException(HttpStatus.CONFLICT, "이미 사용 중인 아이디입니다");
        }

        UserDto user = UserDto.builder()
                .loginId(request.loginId())
                .password(passwordEncoder.encode(request.password()))   // BCrypt 암호화
                .name(request.name())
                .email(request.email())
                .phone(request.phone())
                .roleCode(request.roleCode() != null ? request.roleCode() : "STUDENT")
                .statusCode("ACTIVE")
                .build();

        userMapper.insertUser(user);   // useGeneratedKeys로 userId 채워짐
        return UserResponse.from(userMapper.selectByUserId(user.getUserId()));
    }

    /** 로그인: 인증 성공 시 Access + Refresh Token 발급 */
    public AuthResponse login(LoginRequest request) {
        // CustomUserDetailsService → UserMapper → PasswordEncoder.matches() 흐름으로 검증
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.loginId(), request.password())
        );

        UserDto user = userMapper.selectByLoginId(request.loginId());
        if (user == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "로그인 정보가 올바르지 않습니다");
        }

        userMapper.updateLastLoginAt(user.getUserId());
        return issueTokens(user);
    }

    /** Refresh Token으로 재발급 (rotation: 기존 토큰 폐기 후 새로 발급) */
    public AuthResponse refresh(TokenRefreshRequest request) {
        UserDto user = refreshTokenService.verifyAndGetUser(request.refreshToken());
        refreshTokenService.revoke(request.refreshToken());
        return issueTokens(user);
    }

    /** 로그아웃: 서버 측 Refresh Token 폐기 (Access Token은 클라이언트가 삭제) */
    public void logout(TokenRefreshRequest request) {
        refreshTokenService.revoke(request.refreshToken());
    }

    private AuthResponse issueTokens(UserDto user) {
        String accessToken = jwtService.createAccessToken(user);
        String refreshToken = refreshTokenService.createRefreshToken(user);

        return AuthResponse.bearer(
                accessToken,
                refreshToken,
                jwtService.getAccessTokenExpiresInSeconds(),
                UserResponse.from(user)
        );
    }
}
