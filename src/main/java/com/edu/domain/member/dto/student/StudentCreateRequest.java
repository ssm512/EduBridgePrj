package com.edu.domain.member.dto.student;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * STU-01 학생 등록 요청
 * POST /students
 * users(계정) + students(상세) 두 테이블에 함께 INSERT 된다.
 */
public record StudentCreateRequest(

        /** 계정 정보 (명세서의 userInfo) */
        @NotNull(message = "계정 정보(userInfo)는 필수입니다")
        @Valid
        UserInfo userInfo,

        /** 내부 관리번호(학번) */
        @Size(max = 30, message = "학번은 30자 이하여야 합니다")
        String studentNo,

        /** 생년월일 */
        LocalDate birthDate,

        /** 학교명 */
        @Size(max = 100, message = "학교명은 100자 이하여야 합니다")
        String schoolName,

        /** 학년 */
        @Size(max = 50, message = "학년은 50자 이하여야 합니다")
        String gradeLevel
) {
    /** 학생 계정(users) 생성 정보 */
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
