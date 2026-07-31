package com.edu.domain.member.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * USER-03 회원 수정 요청
 * PUT /api/users/{userId}
 */
public record UserUpdateRequest(

        @NotBlank(message = "이름은 필수입니다")
        @Size(max = 50, message = "이름은 50자 이하여야 합니다")
        String name,

        @Email(message = "이메일 형식이 올바르지 않습니다")
        @Size(max = 255)
        String email,

        @Size(max = 20, message = "연락처는 20자 이하여야 합니다")
        String phone,

        @NotBlank(message = "상태코드는 필수입니다")
        @Pattern(regexp = "ACTIVE|INACTIVE|WITHDRAWN", message = "상태코드는 ACTIVE/INACTIVE/WITHDRAWN 중 하나여야 합니다")
        String statusCode
) {
}
