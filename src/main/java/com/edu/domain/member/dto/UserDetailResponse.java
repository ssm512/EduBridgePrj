package com.edu.domain.member.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * 회원관리용 회원 응답 DTO (비밀번호 제외)
 * USER-01 목록 / USER-02 상세 / USER-03 수정 결과 공용
 */
public record UserDetailResponse(
        Long userId,
        String loginId,
        String name,
        String email,
        String phone,
        String roleCode,
        String statusCode,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime lastLoginAt,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime createdAt,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime updatedAt
) {
    public static UserDetailResponse from(UserDto user) {
        return new UserDetailResponse(
                user.getUserId(),
                user.getLoginId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getRoleCode(),
                user.getStatusCode(),
                user.getLastLoginAt(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
