package com.edu.domain.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 셀프 비밀번호 재설정 요청 (POST /api/auth/password-reset, 비로그인)
 * 아이디 + 이메일이 모두 일치할 때만 임시 비밀번호가 발급된다.
 * (계정 존재 여부를 노출하지 않기 위해 응답은 항상 동일)
 */
public record PasswordResetRequest(
        @NotBlank(message = "아이디는 필수입니다")
        String loginId,

        @NotBlank(message = "이메일은 필수입니다")
        @Email(message = "이메일 형식이 올바르지 않습니다")
        String email
) {
}
