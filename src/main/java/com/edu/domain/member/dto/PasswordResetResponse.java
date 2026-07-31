package com.edu.domain.member.dto;

/**
 * 관리자 비밀번호 초기화 응답 (A안)
 * 임시 비밀번호는 이 응답에서 한 번만 노출된다. (DB에는 BCrypt 해시만 저장)
 */
public record PasswordResetResponse(
        Long userId,
        String loginId,
        String tempPassword
) {
}
