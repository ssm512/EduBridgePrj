package com.edu.domain.member.dto.teacher;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * TEA-03 강사 수정 요청
 * PUT /teachers/{teacherId}
 * 기본정보(users)와 담당과목/입사일(teachers)을 함께 수정한다.
 */
public record TeacherUpdateRequest(

        @NotBlank(message = "이름은 필수입니다")
        @Size(max = 50)
        String name,

        @Email(message = "이메일 형식이 올바르지 않습니다")
        @Size(max = 255)
        String email,

        @Size(max = 20)
        String phone,

        @Size(max = 100, message = "담당 과목은 100자 이하여야 합니다")
        String subject,

        LocalDate hireDate
) {
}
