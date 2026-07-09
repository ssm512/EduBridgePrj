package com.edu.domain.auth.dto;

import com.edu.domain.member.dto.UserDto;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * 비밀번호를 제외한 회원 응답 DTO
 */
public record UserResponse(
        Long userId,
        String loginId,
        String name,
        String email,
        String phone,
        String roleCode,
        String statusCode,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime createdAt
) {
    public static UserResponse from(UserDto user) {
        return new UserResponse(
                user.getUserId(),
                user.getLoginId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getRoleCode(),
                user.getStatusCode(),
                user.getCreatedAt()
        );
    }
}
