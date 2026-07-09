package com.edu.domain.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 간단한 회원가입 요청 DTO
 * roleCode를 생략하면 STUDENT로 가입된다.
 */
public record SignupRequest(
        @NotBlank(message = "아이디는 필수입니다")
        @Size(max = 50, message = "아이디는 50자 이하입니다")
        String loginId,

        @NotBlank(message = "비밀번호는 필수입니다")
        @Size(min = 4, max = 100, message = "비밀번호는 4~100자입니다")
        String password,

        @NotBlank(message = "이름은 필수입니다")
        @Size(max = 50, message = "이름은 50자 이하입니다")
        String name,

        @Email(message = "이메일 형식이 올바르지 않습니다")
        @Size(max = 255, message = "이메일은 255자 이하입니다")
        String email,

        @Size(max = 20, message = "연락처는 20자 이하입니다")
        String phone,

        @Pattern(regexp = "TEACHER|STUDENT|PARENT",
                 message = "roleCode는 TEACHER/STUDENT/PARENT 중 하나입니다")
        String roleCode
) {
}
