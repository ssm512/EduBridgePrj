package com.edu.domain.member.dto.parent;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * 학부모 응답 DTO (PAR-01 등록 결과 / PAR-02 목록 공용)
 */
public record ParentResponse(
        Long parentId,
        Long userId,
        String loginId,
        String name,
        String email,
        String phone,
        String address,
        String statusCode,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime createdAt
) {
    public static ParentResponse from(ParentDto p) {
        return new ParentResponse(
                p.getParentId(),
                p.getUserId(),
                p.getLoginId(),
                p.getName(),
                p.getEmail(),
                p.getPhone(),
                p.getAddress(),
                p.getStatusCode(),
                p.getCreatedAt()
        );
    }
}
