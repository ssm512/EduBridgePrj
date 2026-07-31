package com.edu.domain.member.dto.parent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * parents + users 조인 결과 DTO (MyBatis 매핑용)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParentDto {

    /** 학부모 PK */
    private Long parentId;

    /** 계정 PK (users FK) */
    private Long userId;

    /** 주소 */
    private String address;

    /** 학부모 등록일 */
    private LocalDateTime createdAt;

    // ----- users 조인 컬럼 -----

    /** 로그인 ID */
    private String loginId;

    /** 이름 */
    private String name;

    /** 이메일 */
    private String email;

    /** 연락처 */
    private String phone;

    /** 계정 상태 (ACTIVE/INACTIVE/WITHDRAWN) */
    private String statusCode;
}
