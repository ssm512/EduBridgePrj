package com.edu.domain.member.dto.teacher;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * TEA-01 강사 등록 요청
 * POST /api/teachers
 * users(계정) + teachers(상세) 두 테이블에 함께 INSERT 된다.
 */
public record TeacherCreateRequest(

        /** 계정 정보 (명세서의 userInfo) */
        @NotNull(message = "계정 정보(userInfo)는 필수입니다")
        @Valid
        UserInfo userInfo,

        /** 담당 과목 */
        @Size(max = 100, message = "담당 과목은 100자 이하여야 합니다")
        String subject,

        /** 입사일 */
        LocalDate hireDate
) {
    /** 강사 계정(users) 생성 정보 */
    public record UserInfo(
            @NotBlank(message = "로그인 ID는 필수입니다")
            @Size(max = 50)
            String loginId,

            @NotBlank(message = "비밀번호는 필수입니다")
            @Size(min = 4, max = 100, message = "비밀번호는 4자 이상이어야 합니다")
            String password,

            @NotBlank(message = "이름은 필수입니다")
            @Size(max = 50)
            String name,

            @Email(message = "이메일 형식이 올바르지 않습니다")
            @Size(max = 255)
            String email,

            @Size(max = 20)
            String phone
    ) {
    }
}
