package com.edu.domain.auth.service;

import com.edu.common.exception.ApiException;
import com.edu.config.JwtProperties;
import com.edu.domain.auth.mapper.RefreshTokenMapper;
import com.edu.domain.auth.vo.RefreshTokenVo;
import com.edu.domain.member.dto.UserDto;
import com.edu.domain.member.mapper.UserMapper;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

/**
 * Refresh Token 생성/검증/폐기 서비스.
 * - 원문(랜덤 문자열)은 클라이언트에게만 전달
 * - DB(refresh_tokens.refresh_token)에는 SHA-256 해시만 저장
 * - 사용자 1명당 활성 토큰 1개 정책
 */
@Service
@Transactional
public class RefreshTokenService {

    private final RefreshTokenMapper refreshTokenMapper;
    private final UserMapper userMapper;
    private final JwtProperties jwtProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(RefreshTokenMapper refreshTokenMapper,
                               UserMapper userMapper,
                               JwtProperties jwtProperties) {
        this.refreshTokenMapper = refreshTokenMapper;
        this.userMapper = userMapper;
        this.jwtProperties = jwtProperties;
    }

    /** Refresh Token 발급 (원문 반환, DB에는 해시 저장) */
    public String createRefreshToken(UserDto user) {
        // 기존 활성 토큰 전부 폐기
        refreshTokenMapper.revokeAllByUserId(user.getUserId());

        String rawToken = createRandomToken();

        RefreshTokenVo token = RefreshTokenVo.builder()
                .userId(user.getUserId())
                .refreshToken(sha256(rawToken))
                .expiresAt(LocalDateTime.now().plusDays(jwtProperties.refreshTokenDays()))
                .revokedYn("N")
                .build();
        refreshTokenMapper.insertToken(token);

        return rawToken;
    }

    /** Refresh Token 검증 후 사용자 반환 */
    @Transactional(readOnly = true)
    public UserDto verifyAndGetUser(String rawToken) {
        RefreshTokenVo token = refreshTokenMapper.selectByTokenHash(sha256(rawToken));
        if (token == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "유효하지 않은 Refresh Token입니다");
        }
        if (token.isRevoked()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "이미 폐기된 Refresh Token입니다");
        }
        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "만료된 Refresh Token입니다");
        }

        UserDto user = userMapper.selectByUserId(token.getUserId());
        if (user == null || !"ACTIVE".equals(user.getStatusCode())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "비활성화된 사용자입니다");
        }
        return user;
    }

    /** Refresh Token 폐기 (없어도 조용히 종료) */
    public void revoke(String rawToken) {
        refreshTokenMapper.revokeByTokenHash(sha256(rawToken));
    }

    /** 매일 새벽 3시 만료 토큰 삭제 */
    @Scheduled(cron = "0 0 3 * * *")
    public void deleteExpiredTokens() {
        refreshTokenMapper.deleteExpiredTokens(LocalDateTime.now());
    }

    private String createRandomToken() {
        byte[] bytes = new byte[64];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다", e);
        }
    }
}
