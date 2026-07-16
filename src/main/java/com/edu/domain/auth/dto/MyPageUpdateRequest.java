package com.edu.domain.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 마이페이지 본인 정보 수정 요청 (PUT /api/auth/me)
 *
 * 이름/이메일/연락처만 수정 가능하다.
 * 아이디(login_id)는 수정 대상에서 제외 - JWT accessToken의 subject가 login_id라
 * 바꾸면 즉시 토큰 재발급/재로그인 처리가 필요해지므로 이번 범위에서는 다루지 않는다.
 * 권한(roleCode)/상태(statusCode)도 여기서 바꿀 수 없다 (statusCode는 ADMIN 전용 그대로 유지).
 */
public record MyPageUpdateRequest(

        @NotBlank(message = "이름은 필수입니다")
        @Size(max = 50, message = "이름은 50자 이하여야 합니다")
        String name,

        @NotBlank(message = "이메일은 필수입니다")
        @Email(message = "이메일 형식이 올바르지 않습니다")
        @Size(max = 255)
        String email,

        @Size(max = 20, message = "연락처는 20자 이하여야 합니다")
        String phone
) {
}
