package com.edu.domain.member.dto.student;

import lombok.Data;

/**
 * STU-03 상세의 보호자 정보 (student_parents + parents + users 조인, MyBatis 매핑용)
 */
@Data
public class GuardianDto {

    /** 학생-학부모 연결 PK */
    private Long studentParentId;

    /** 학부모 PK */
    private Long parentId;

    /** 학부모 로그인 ID */
    private String loginId;

    /** 보호자 이름 */
    private String name;

    /** 보호자 연락처 */
    private String phone;

    /** 관계 (FATHER/MOTHER/GUARDIAN) */
    private String relationCode;

    /** 주 보호자 여부 (Y/N) */
    private String primaryYn;
}
