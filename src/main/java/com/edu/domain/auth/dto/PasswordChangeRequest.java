package com.edu.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 본인 비밀번호 변경 요청 (PUT /api/auth/password)
 * 현재 비밀번호 확인 후 새 비밀번호로 교체한다.
 */
public record PasswordChangeRequest(
        @NotBlank(message = "현재 비밀번호는 필수입니다")
        String currentPassword,

        @NotBlank(message = "새 비밀번호는 필수입니다")
        @Size(min = 4, max = 100, message = "비밀번호는 4~100자입니다")
        String newPassword
) {
}
