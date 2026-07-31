package com.edu.domain.member.dto.student;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * students + users 조인 결과 DTO (MyBatis 매핑용)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentDto {

    /** 학생 PK */
    private Long studentId;

    /** 계정 PK (users FK) */
    private Long userId;

    /** 내부 관리번호(학번) */
    private String studentNo;

    /** 생년월일 */
    private LocalDate birthDate;

    /** 학교명 */
    private String schoolName;

    /** 학년 */
    private String gradeLevel;

    /** 메모 */
    private String memo;

    /** 학생 등록일 */
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
