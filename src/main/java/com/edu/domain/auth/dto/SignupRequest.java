package com.edu.domain.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 셀프 회원가입 요청 DTO
 * - roleCode를 생략하면 STUDENT로 가입된다.
 * - 이메일은 비밀번호 재설정 등에 사용되므로 필수.
 * - 역할별 추가 정보: 학생(schoolName/gradeLevel), 학부모(address), 강사(subject)
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

        @NotBlank(message = "이메일은 필수입니다")
        @Email(message = "이메일 형식이 올바르지 않습니다")
        @Size(max = 255, message = "이메일은 255자 이하입니다")
        String email,

        @Size(max = 20, message = "연락처는 20자 이하입니다")
        String phone,

        @Pattern(regexp = "TEACHER|STUDENT|PARENT",
                 message = "roleCode는 TEACHER/STUDENT/PARENT 중 하나입니다")
        String roleCode,

        // ── 역할별 추가 정보 (역할에 해당하는 값만 사용) ──

        /* STUDENT */
        @Size(max = 100, message = "학교명은 100자 이하입니다")
        String schoolName,

        @Size(max = 50, message = "학년은 50자 이하입니다")
        String gradeLevel,

        /* PARENT */
        @Size(max = 255, message = "주소는 255자 이하입니다")
        String address,

        /* TEACHER */
        @Size(max = 100, message = "과목은 100자 이하입니다")
        String subject
) {
}
