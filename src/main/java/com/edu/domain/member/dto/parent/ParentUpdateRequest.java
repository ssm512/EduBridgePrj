package com.edu.domain.member.dto.parent;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 학부모 수정 요청 (명세서 외 추가 API)
 * PUT /api/parents/{parentId}
 * 기본정보(users)와 주소(parents)를 함께 수정한다.
 */
public record ParentUpdateRequest(

        @NotBlank(message = "이름은 필수입니다")
        @Size(max = 50)
        String name,

        @Email(message = "이메일 형식이 올바르지 않습니다")
        @Size(max = 255)
        String email,

        @Size(max = 20)
        String phone,

        @Size(max = 255, message = "주소는 255자 이하여야 합니다")
        String address
) {
}
