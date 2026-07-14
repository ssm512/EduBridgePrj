package com.edu.domain.member.dto.teacher;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * teachers + users 조인 결과 DTO (MyBatis 매핑용)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherDto {

    /** 강사 PK */
    private Long teacherId;

    /** 계정 PK (users FK) */
    private Long userId;

    /** 담당 과목 */
    private String subject;

    /** 입사일 */
    private LocalDate hireDate;

    /** 강사 등록일 */
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
