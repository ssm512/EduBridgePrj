package com.edu.domain.member.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {

    /** 사용자 PK */
    private Long userId;

    /** 로그인 ID */
    private String loginId;

    /** BCrypt 암호화 비밀번호 */
    private String password;

    /** 사용자 이름 */
    private String name;

    /** 이메일 */
    private String email;

    /** 연락처 */
    private String phone;

    /** 권한 (ADMIN, TEACHER, STUDENT, PARENT) */
    private String roleCode;

    /** 상태 (ACTIVE, INACTIVE, WITHDRAWN) */
    private String statusCode;

    /** 관리자 초기화 후 비밀번호 강제 변경 필요 여부 */
    private Boolean mustChangePassword;

    /** 마지막 로그인 시간 */
    private LocalDateTime lastLoginAt;

    /** 생성일 */
    private LocalDateTime createdAt;

    /** 수정일 */
    private LocalDateTime updatedAt;
}